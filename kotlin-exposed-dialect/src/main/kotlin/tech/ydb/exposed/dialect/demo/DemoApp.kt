package tech.ydb.exposed.dialect.demo

import org.jetbrains.exposed.v1.core.asLiteral
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.ydb.exposed.dialect.basic.YdbDialectProvider
import tech.ydb.exposed.dialect.types.ydbDecimalLiteral
import java.math.BigDecimal

fun main() {
    val db = YdbDialectProvider.connect(
        url = "jdbc:ydb:grpc://localhost:2136/local",
        driver = "tech.ydb.jdbc.YdbDriver"
    )

    transaction(db) {
        println("== Подготовка схемы ==")
        SchemaUtils.drop(DemoProducts)
        SchemaUtils.create(DemoProducts)

        println("DDL:")
        DemoProducts.ddl.forEach { println(it) }

        println()
        println("== CREATE ==")
        seedDemoData()

        DemoProducts.selectAll()
            .orderBy(DemoProducts.id)
            .forEach {
                println("product[id=${it[DemoProducts.id]}, sku=${it[DemoProducts.sku]}, name=${it[DemoProducts.name]}, category=${it[DemoProducts.category]}, price=${it[DemoProducts.price]}]")
            }

        println()
        println("== READ по category ==")
        DemoProducts.selectAll()
            .where { DemoProducts.category eq "books" }
            .orderBy(DemoProducts.id)
            .forEach {
                println("books -> ${it[DemoProducts.name]} (${it[DemoProducts.price]})")
            }

        println()
        println("== UPDATE ==")
        DemoProducts.update({ DemoProducts.sku eq "BOOK-002" }) {
            it[DemoProducts.name] = "Distributed Systems, 2nd edition"
            it.update(
                DemoProducts.price,
                ydbDecimalLiteral(BigDecimal("45.00"), 10, 2)
            )
        }

        DemoProducts.selectAll()
            .where { DemoProducts.sku eq "BOOK-002" }
            .forEach {
                println("updated -> ${it[DemoProducts.name]} (${it[DemoProducts.price]})")
            }

        println()
        println("== KEYSET PAGINATION ==")
        val page1 = DemoProducts
            .selectAll()
            .keysetPage(DemoProducts.id, lastValue = null, limit = 2)
            .toList()

        println("page1:")
        page1.forEach {
            println("  ${it[DemoProducts.id]} -> ${it[DemoProducts.name]}")
        }

        val lastSeenId = page1.last()[DemoProducts.id]

        val page2 = DemoProducts
            .selectAll()
            .keysetPage(DemoProducts.id, lastValue = lastSeenId, limit = 2)
            .toList()

        println("page2:")
        page2.forEach {
            println("  ${it[DemoProducts.id]} -> ${it[DemoProducts.name]}")
        }

        println()
        println("== DELETE ==")
        DemoProducts.deleteWhere { DemoProducts.sku eq "HW-001" }

        DemoProducts.selectAll()
            .orderBy(DemoProducts.id)
            .forEach {
                println("remaining -> ${it[DemoProducts.id]} / ${it[DemoProducts.sku]} / ${it[DemoProducts.name]}")
            }
    }
}

private fun seedDemoData() {
    DemoProducts.insert {
        it[id] = 1
        it[sku] = "BOOK-001"
        it[name] = "Kotlin in Action"
        it[category] = "books"
        it[price] = BigDecimal("39.90")
    }

    DemoProducts.insert {
        it[id] = 2
        it[sku] = "BOOK-002"
        it[name] = "Distributed Systems"
        it[category] = "books"
        it[price] = BigDecimal("42.50")
    }

    DemoProducts.insert {
        it[id] = 3
        it[sku] = "HW-001"
        it[name] = "Mechanical Keyboard"
        it[category] = "hardware"
        it[price] = BigDecimal("129.99")
    }

    DemoProducts.insert {
        it[id] = 4
        it[sku] = "HW-002"
        it[name] = "USB-C Dock"
        it[category] = "hardware"
        it[price] = BigDecimal("89.00")
    }
}