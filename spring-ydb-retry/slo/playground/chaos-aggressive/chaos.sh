#!/bin/sh -e

get_random_container() {
    # Get a list of all containers starting with ydb-database-*
    containers=$(docker ps --format '{{.Names}}' | grep '^ydb-database-')

    # Convert the list to a newline-separated string
    containers=$(echo "$containers" | tr ' ' '\n')

    # Count the number of containers
    containersCount=$(echo "$containers" | wc -l)

    # Generate a random number between 0 and containersCount - 1
    randomIndex=$(shuf -i 0-$(($containersCount - 1)) -n 1)

    # Get the container name at the random index
    nodeForChaos=$(echo "$containers" | sed -n "$(($randomIndex + 1))p")
}

get_two_random_containers() {
    containers=$(docker ps --format '{{.Names}}' | grep '^ydb-database-')
    containers=$(echo "$containers" | tr ' ' '\n')
    containersCount=$(echo "$containers" | wc -l)
    if [ "$containersCount" -lt 2 ]; then
        get_random_container
        nodeForChaos2=""
        return
    fi
    randomIndex1=$(shuf -i 0-$(($containersCount - 1)) -n 1)
    randomIndex2=$(shuf -i 0-$(($containersCount - 2)) -n 1)
    if [ "$randomIndex2" -ge "$randomIndex1" ]; then
        randomIndex2=$(($randomIndex2 + 1))
    fi
    nodeForChaos=$(echo "$containers" | sed -n "$(($randomIndex1 + 1))p")
    nodeForChaos2=$(echo "$containers" | sed -n "$(($randomIndex2 + 1))p")
}

sleep 60

echo "Start AGGRESSIVE CHAOS on YDB cluster!"

# Phase 1: Pause/unpause
echo "=== Phase 1: docker pause/unpause ==="
for i in $(seq 1 4)
do
  get_random_container
  echo "[$(date)]: PAUSE ${nodeForChaos} (iteration $i) — in-flight ops will hang"
  docker pause ${nodeForChaos}
  sleep 20
  echo "[$(date)]: UNPAUSE ${nodeForChaos}"
  docker unpause ${nodeForChaos}
  sleep 15
done

# Phase 2: Multi-node simultaneous kill
echo "=== Phase 2: multi-node kill ==="
for i in $(seq 1 3)
do
  get_two_random_containers
  echo "[$(date)]: KILL ${nodeForChaos} and ${nodeForChaos2} simultaneously (iteration $i)"
  docker kill -s SIGKILL ${nodeForChaos} &
  docker kill -s SIGKILL ${nodeForChaos2} &
  wait
  echo "[$(date)]: Starting both nodes back"
  docker start ${nodeForChaos} &
  docker start ${nodeForChaos2} &
  wait
  sleep 25
done

# Phase 3: Single-node instant restart
echo "=== Phase 3: instant restart ==="
for i in $(seq 1 3)
do
  get_random_container
  echo "[$(date)]: INSTANT RESTART ${nodeForChaos} (iteration $i)"
  docker restart ${nodeForChaos} -t 0
  sleep 20
done

# Phase 4: Pause 2 nodes simultaneously
echo "=== Phase 4: dual pause 30s ==="
get_two_random_containers
echo "[$(date)]: PAUSE ${nodeForChaos} and ${nodeForChaos2} for 30s"
docker pause ${nodeForChaos} &
docker pause ${nodeForChaos2} &
wait
sleep 30
echo "[$(date)]: UNPAUSE both"
docker unpause ${nodeForChaos} &
docker unpause ${nodeForChaos2} &
wait
sleep 15

# Phase 5: Rapid kill/start cycle (session pool thrashing)
echo "=== Phase 5: rapid kill/start ==="
for i in $(seq 1 5)
do
  get_random_container
  echo "[$(date)]: RAPID kill+start ${nodeForChaos} (iteration $i)"
  docker kill -s SIGKILL ${nodeForChaos}
  docker start ${nodeForChaos}
  sleep 8
done

# Phase 6: Final triple kill
echo "=== Phase 6: FINAL triple SIGKILL ==="
containers=$(docker ps --format '{{.Names}}' | grep '^ydb-database-' | shuf)
c1=$(echo "$containers" | sed -n '1p')
c2=$(echo "$containers" | sed -n '2p')
c3=$(echo "$containers" | sed -n '3p')
echo "[$(date)]: SIGKILL ${c1}, ${c2}, ${c3} simultaneously"
docker kill -s SIGKILL ${c1} &
docker kill -s SIGKILL ${c2} &
docker kill -s SIGKILL ${c3} &
wait

echo "[$(date)]: Chaos complete."
