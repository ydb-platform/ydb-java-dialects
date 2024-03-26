package stress_test

import (
	"bytes"
	"context"
	"fmt"
	"github.com/stretchr/testify/assert"
	"github.com/ydb-platform/ydb-go-sdk/v3"
	"github.com/ydb-platform/ydb-go-sdk/v3/table"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/options"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/result"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/result/named"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/types"
	"log"
	"os/exec"
	"sort"
	"strings"
	"sync"
	"testing"
	"time"
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

	ctx := context.Background()

	db, err := ydb.Open(ctx, "grpc://localhost:2136/local")
	if err != nil {
		t.Fatal(err)
	}

	err = db.Table().Do(ctx,
		func(ctx context.Context, s table.Session) (err error) {
			describeTable, err := s.DescribeTable(ctx, "/local/new_episodes")
			if err != nil {
				return err
			}

			// check type columns
			expectedColumns := []options.Column{
				{Name: "series_id", Type: types.TypeInt64, Family: ""},
				{Name: "season_id", Type: types.TypeInt64, Family: ""},
				{Name: "episode_id", Type: types.TypeInt64, Family: ""},
				{Name: "title", Type: types.Optional(types.TypeText), Family: ""},
				{Name: "air_date", Type: types.Optional(types.TypeTimestamp), Family: ""},
				{Name: "deleted", Type: types.Optional(types.TypeBool), Family: ""},
			}

			sort.Slice(expectedColumns, func(i, j int) bool {
				return expectedColumns[i].Name < expectedColumns[j].Name
			})

			actualColumns := describeTable.Columns

			sort.Slice(actualColumns, func(i, j int) bool {
				return actualColumns[i].Name < actualColumns[j].Name
			})

			assert.Equal(t, expectedColumns, describeTable.Columns)

			assert.Equal(t, "episodes_index", describeTable.Indexes[0].Name)
			return
		},
	)

	if err != nil {
		t.Fatal(err)
	}

	err = db.Table().Do(ctx,
		func(ctx context.Context, s table.Session) (err error) {
			_, res, err := s.Execute(ctx, table.DefaultTxControl(),
				`SELECT * FROM all_types_table`,
				nil, // empty parameters
			)

			if err != nil {
				return err
			}

			defer func(res result.Result) {
				e := res.Close()
				if e != nil {
					err = e
				}
			}(res)

			var (
				id             int
				boolValue      *bool
				smallIntValue  *int16
				tinyIntValue   *int8
				bigIntValue    *int64
				floatValue     *float32
				doubleValue    *float64
				decimalValue   *types.Decimal
				uint8Value     *uint8
				uint16Value    *uint16
				uint32Value    *uint32
				uint64Value    *uint64
				textValue      *string
				binaryValue    *[]byte
				jsonValue      *string
				jsonDocValue   *string
				dateValue      *time.Time
				datetimeValue  *time.Time
				timestampValue *time.Time
			)

			for res.NextResultSet(ctx) {
				for res.NextRow() {
					err := res.ScanNamed(
						named.Required("id", &id),
						named.Optional("bool_column", &boolValue),
						named.Optional("smallint_column", &smallIntValue),
						named.Optional("tinyint_column", &tinyIntValue),
						named.Optional("bigint_column", &bigIntValue),
						named.Optional("float_column", &floatValue),
						named.Optional("double_column", &doubleValue),
						named.Optional("decimal_column", &decimalValue),
						named.Optional("uint8_column", &uint8Value),
						named.Optional("uint16_column", &uint16Value),
						named.Optional("uint32_column", &uint32Value),
						named.Optional("uint64_column", &uint64Value),
						named.Optional("text_column", &textValue),
						named.Optional("binary_column", &binaryValue),
						named.Optional("json_column", &jsonValue),
						named.Optional("jsondocument_column", &jsonDocValue),
						named.Optional("date_column", &dateValue),
						named.Optional("datetime_column", &datetimeValue),
						named.Optional("timestamp_column", &timestampValue),
					)

					if err != nil {
						t.Error(err)
					}
				}
			}

			assert.Equal(t, 3, id)
			assert.Equal(t, true, *boolValue)
			assert.Equal(t, int16(13000), *smallIntValue)
			assert.Equal(t, int8(112), *tinyIntValue)
			assert.Equal(t, int64(123123), *bigIntValue)
			assert.Equal(t, float32(1.123000026), *floatValue)
			assert.Equal(t, 1.123123, *doubleValue)
			assert.Equal(t, "1.123123000", (*decimalValue).String())
			assert.Equal(t, uint8(12), *uint8Value)
			assert.Equal(t, uint16(13), *uint16Value)
			assert.Equal(t, uint32(14), *uint32Value)
			assert.Equal(t, uint64(15), *uint64Value)
			assert.Equal(t, "Кирилл Курдюков Алексеевич", *textValue)
			assert.Equal(t, "binary", string(*binaryValue))
			assert.Equal(t, "[1, 2, 3]", *jsonValue)
			assert.Equal(t, "[1,2,3]", *jsonDocValue)
			assert.Equal(t, "2014-04-06", strings.Split(dateValue.String(), " ")[0])
			assert.Equal(t, "2023-09-16 15:30:00",
				strings.Split(datetimeValue.String(), " ")[0]+" "+strings.Split(datetimeValue.String(), " ")[1])
			assert.Equal(t, "2023-07-31 20:00:00.123123",
				strings.Split(timestampValue.String(), " ")[0]+" "+strings.Split(timestampValue.String(), " ")[1])

			return
		},
	)

	if err != nil {
		t.Fatal(err)
	}
}

func migrate(changelogNum int, t *testing.T) {
	cmd := exec.Command("liquibase-cli/liquibase", "update",
		fmt.Sprintf("--changelog-file=changelog/changelog-%v.xml", changelogNum),
		"--url=jdbc:ydb:grpc://localhost:2136/local")

	var stdout bytes.Buffer

	cmd.Stdout = &stdout

	err := cmd.Run()

	if err != nil {
		t.Errorf("liquibase update failed: %v", err)

		log.Println(stdout.String())

		return
	}

	out := stdout.String()

	if !strings.Contains(out, "Liquibase command 'update' was executed successfully.") {
		t.Errorf(out)
	}
}
