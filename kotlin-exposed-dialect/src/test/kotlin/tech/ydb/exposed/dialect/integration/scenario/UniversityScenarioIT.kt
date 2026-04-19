package tech.ydb.exposed.dialect.integration.scenario

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class UniversityScenarioIT : BaseYdbTest() {

    object Departments : YdbTable("departments") {
        val id = integer("id")
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)
    }

    object Students : YdbTable("students") {
        val id = integer("id")
        val name = varchar("name", 255)
        val departmentId = integer("department_id")

        override val primaryKey = PrimaryKey(id)
    }

    object Courses : YdbTable("courses") {
        val id = integer("id")
        val name = varchar("name", 255)
        val departmentId = integer("department_id")

        override val primaryKey = PrimaryKey(id)
    }

    object Enrollments : YdbTable("enrollments") {
        val id = integer("id")
        val studentId = integer("student_id")
        val courseId = integer("course_id")
        val semester = varchar("semester", 64)

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(
        Departments,
        Students,
        Courses,
        Enrollments
    )

    private fun seedData() {
        Departments.insert {
            it[id] = 1
            it[name] = "Computer Science"
        }
        Departments.insert {
            it[id] = 2
            it[name] = "Mathematics"
        }

        Students.insert {
            it[id] = 1
            it[name] = "Alice"
            it[departmentId] = 1
        }
        Students.insert {
            it[id] = 2
            it[name] = "Bob"
            it[departmentId] = 1
        }
        Students.insert {
            it[id] = 3
            it[name] = "Carol"
            it[departmentId] = 2
        }

        Courses.insert {
            it[id] = 1
            it[name] = "Algorithms"
            it[departmentId] = 1
        }
        Courses.insert {
            it[id] = 2
            it[name] = "Databases"
            it[departmentId] = 1
        }
        Courses.insert {
            it[id] = 3
            it[name] = "Linear Algebra"
            it[departmentId] = 2
        }

        Enrollments.insert {
            it[id] = 1
            it[studentId] = 1
            it[courseId] = 1
            it[semester] = "2026-spring"
        }
        Enrollments.insert {
            it[id] = 2
            it[studentId] = 1
            it[courseId] = 2
            it[semester] = "2026-spring"
        }
        Enrollments.insert {
            it[id] = 3
            it[studentId] = 2
            it[courseId] = 1
            it[semester] = "2026-spring"
        }
        Enrollments.insert {
            it[id] = 4
            it[studentId] = 3
            it[courseId] = 3
            it[semester] = "2026-spring"
        }
    }

    @Test
    fun `should load computer science students with their enrolled courses`() = tx {
        seedData()

        val rows = Students
            .join(Departments, JoinType.INNER, Students.departmentId, Departments.id)
            .join(Enrollments, JoinType.INNER, Students.id, Enrollments.studentId)
            .join(Courses, JoinType.INNER, Enrollments.courseId, Courses.id)
            .selectAll()
            .where { Departments.name eq "Computer Science" }
            .map {
                Triple(
                    it[Students.name],
                    it[Courses.name],
                    it[Enrollments.semester]
                )
            }
            .sortedWith(compareBy({ it.first }, { it.second }))

        assertEquals(
            listOf(
                Triple("Alice", "Algorithms", "2026-spring"),
                Triple("Alice", "Databases", "2026-spring"),
                Triple("Bob", "Algorithms", "2026-spring")
            ),
            rows
        )
    }

    @Test
    fun `should load spring roster for algorithms course`() = tx {
        seedData()

        val rows = Courses
            .join(Enrollments, JoinType.INNER, Courses.id, Enrollments.courseId)
            .join(Students, JoinType.INNER, Enrollments.studentId, Students.id)
            .selectAll()
            .where {
                (Courses.name eq "Algorithms") and
                        (Enrollments.semester eq "2026-spring")
            }
            .map { it[Students.name] }
            .sorted()

        assertEquals(listOf("Alice", "Bob"), rows)
    }
}