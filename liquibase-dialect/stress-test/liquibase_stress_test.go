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
	migrate(0, t) // init DATABASECHANGELOG, DATABASECHANGELOGLOCK

	for changelogNum := 1; changelogNum <= 6; changelogNum++ {
		wg := sync.WaitGroup{}

		for i := 0; i < 10; i++ {
			curIteration := i

			wg.Add(1)

			go func() {
				defer wg.Done()

				log.Println("Execute goroutine with num: ", curIteration)

				migrate(changelogNum, t)
			}()
		}

		wg.Wait()
	}
}

func migrate(changelogNum int, t *testing.T) {
	cmd := exec.Command("liquibase-cli/liquibase", "update",
		fmt.Sprintf("--changelog-file=changelog/changelog-%v.xml", changelogNum),
		"--url=jdbc:ydb:grpc://localhost:2136/local")

	var stderr bytes.Buffer

	cmd.Stderr = &stderr

	err := cmd.Run()

	if err != nil {
		t.Errorf("liquibase update failed: %v", err)

		log.Println(stderr.String())

		return
	}

	out := stderr.String()

	if !strings.Contains(out, "Liquibase command 'update' was executed successfully.") {
		t.Errorf(out)
	}
}
