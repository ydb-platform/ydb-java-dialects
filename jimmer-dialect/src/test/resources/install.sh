docker run \
  --restart=always \
  -d \
  --name jimmer_test_ydb \
  -h localhost \
  --platform linux/amd64 \
  -p 2135:2135 \
  -p 2136:2136 \
  -p 8765:8765 \
  -p 9092:9092 \
  -v $(pwd)/ydb_certs:/ydb_certs \
  -v $(pwd)/ydb_data:/ydb_data \
  -e GRPC_TLS_PORT=2135 \
  -e GRPC_PORT=2136 \
  -e MON_PORT=8765 \
  -e YDB_KAFKA_PROXY_PORT=9092 \
  ydbplatform/local-ydb:latest
