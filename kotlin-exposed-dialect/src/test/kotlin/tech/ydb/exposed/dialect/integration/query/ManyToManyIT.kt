package tech.ydb.exposed.dialect.integration.query

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class ManyToManyIT : BaseYdbTest() {

    object Students : YdbTable("students") {
        val id = integer("id")
        val name = varchar("name", 255)
        override val primaryKey = PrimaryKey(id)
    }

    object Courses : YdbTable("courses") {
        val id = integer("id")
        val title = varchar("title", 255)
        override val primaryKey = PrimaryKey(id)
    }

    object StudentCourses : YdbTable("student_courses") {
        val id = integer("id")
        val studentId = integer("student_id")
        val courseId = integer("course_id")
        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Students, Courses, StudentCourses)

    @Test
    fun `should support many to many through link table`() = tx {
        Students.insert {
            it[id] = 1
            it[name] = "Alice"
        }
        Students.insert {
            it[id] = 2
            it[name] = "Bob"
        }

        Courses.insert {
            it[id] = 10
            it[title] = "Math"
        }
        Courses.insert {
            it[id] = 11
            it[title] = "Physics"
        }

        StudentCourses.insert {
            it[id] = 100
            it[studentId] = 1
            it[courseId] = 10
        }
        StudentCourses.insert {
            it[id] = 101
            it[studentId] = 1
            it[courseId] = 11
        }
        StudentCourses.insert {
            it[id] = 102
            it[studentId] = 2
            it[courseId] = 11
        }

        val rows = Students
            .join(
                otherTable = StudentCourses,
                joinType = JoinType.INNER,
                onColumn = Students.id,
                otherColumn = StudentCourses.studentId
            )
            .join(
                otherTable = Courses,
                joinType = JoinType.INNER,
                onColumn = StudentCourses.courseId,
                otherColumn = Courses.id
            )
            .select(Students.name, Courses.title)
            .where { Students.name eq "Alice" }
            .orderBy(Courses.id to SortOrder.ASC)
            .toList()

        assertEquals(2, rows.size)
        assertEquals("Alice", rows[0][Students.name])
        assertEquals("Math", rows[0][Courses.title])
        assertEquals("Alice", rows[1][Students.name])
        assertEquals("Physics", rows[1][Courses.title])
    }
}