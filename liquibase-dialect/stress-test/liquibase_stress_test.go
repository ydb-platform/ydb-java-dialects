package stress_test

import (
	"bytes"
	"fmt"
	"log"
	"os/exec"
	"strings"
	"sync"
	"testing"
)

func TestLiquibaseUpdateStressTest(t *testing.T) {
	wg := sync.WaitGroup{}

	for changelogNum := 1; changelogNum <= 5; changelogNum++ {
		for i := 0; i < 10; i++ {
			curIteration := i

			wg.Add(1)

			go func() {
				defer wg.Done()

				cmd := exec.Command("docker", "run", "--network", "stress-test-network", "--rm",
					"-v", "$(pwd)/changelog:/liquibase/lib",
					"liquibase-ydb", "update",
					fmt.Sprintf("--changelog-file=changelog-%v.xml", changelogNum),
					"--url=jdbc:ydb:grpc://ydb.local:2136/local",
					"--log-level=debug")

				var stderr bytes.Buffer

				cmd.Stderr = &stderr

				log.Println("Execute goroutine with num: %i", curIteration)

				err := cmd.Run()

				log.Println(stderr.String())

				if err != nil {

					t.Errorf("liquibase update failed: %v", err)

					return
				}

				out := stderr.String()

				if !strings.Contains(out, "Liquibase command 'update' was executed successfully.") {
					t.Errorf(out)
				}
			}()
		}

		wg.Wait()
	}
}
