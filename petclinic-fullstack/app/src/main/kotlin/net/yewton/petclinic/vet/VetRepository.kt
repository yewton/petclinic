package net.yewton.petclinic.vet

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.tracing.annotation.NewSpan
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import net.yewton.petclinic.jooq.tables.references.VETS
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.multiset
import org.jooq.impl.DSL.selectFrom
import org.jooq.kotlin.intoList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.observability.micrometer.Micrometer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Objects

@Component
class VetRepository(
  private val create: DSLContext,
  private val client: DatabaseClient,
  private val observationRegistry: ObservationRegistry,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(VetRepository::class.java)
  }

  @NewSpan("findAllCoroutineJooqMethod")
  @Transactional(readOnly = true)
  suspend fun findAllCoroutineJooq(pageable: Pageable): Page<Vet> {
    logBefore(isReactor = false, isSpring = false)
    val observation = Observation.start("findAllCoroutineJooq", observationRegistry).start()
    try {
      val query = findAllQuery(pageable)
      val result =
        Flux
          .from(query)
          .asFlow()
          .map {
            val vet = Vet(it.get(VETS.ID), it.get(VETS.FIRST_NAME), it.get(VETS.LAST_NAME), it.value4())
            val totalCount = it.value5()
            vet to totalCount
          }.toList()
          .toMap()
      logAfter(isReactor = false, isSpring = false)
      return PageImpl(result.keys.toList(), pageable, result.values.first().toLong())
    } finally {
      observation.stop()
    }
  }

  @NewSpan("findAllReactorJooqMethod")
  @Transactional(readOnly = true)
  fun findAllReactorJooq(pageable: Pageable): Mono<Page<Vet>> {
    logBefore(isReactor = true, isSpring = false)
    val query = findAllQuery(pageable)
    return Flux
      .from(query)
      .name("findAllReactorJooq")
      .tap(Micrometer.observation(observationRegistry))
      .map {
        val vet = Vet(it.get(VETS.ID), it.get(VETS.FIRST_NAME), it.get(VETS.LAST_NAME), it.value4())
        val totalCount = it.value5()
        vet to totalCount
      }.collectList()
      .map { it.toMap() }
      .map<Page<Vet>> {
        PageImpl(it.keys.toList(), pageable, it.values.first().toLong())
      }.doOnNext { logAfter(isReactor = true, isSpring = false) }
  }

  @NewSpan("findAllCoroutineSpringMethod")
  @Transactional(readOnly = true)
  suspend fun findAllCoroutineSpring(pageable: Pageable): Page<Vet> {
    logBefore(isReactor = false, isSpring = true)
    val observation = Observation.start("findAllCoroutineSpring", observationRegistry).start()
    try {
      return map(findAllCoreQuery(pageable).flow().toList())
        .let { (totalCount, vets) ->
          logAfter(isReactor = false, isSpring = true)
          PageImpl(vets, pageable, totalCount)
        }
    } finally {
      observation.stop()
    }
  }

  @NewSpan("findAllReactorSpringMethod")
  @Transactional(readOnly = true)
  fun findAllReactorSpring(pageable: Pageable): Mono<Page<Vet>> {
    logBefore(isReactor = true, isSpring = true)
    return findAllCoreQuery(pageable)
      .all()
      .name("findAllReactorSpring")
      .tap(Micrometer.observation(observationRegistry))
      .collectList()
      .flatMap<Page<Vet>> { rows ->
        map(rows).let { (totalCount, vets) ->
          Mono.just(PageImpl(vets, pageable, totalCount))
        }
      }.doOnNext { logAfter(isReactor = true, isSpring = true) }
  }

  private fun logBefore(
    isReactor: Boolean,
    isSpring: Boolean,
  ) = log(isReactor, isSpring, true)

  private fun logAfter(
    isReactor: Boolean,
    isSpring: Boolean,
  ) = log(isReactor, isSpring, false)

  private fun log(
    isReactor: Boolean,
    isSpring: Boolean,
    isBefore: Boolean,
  ) {
    val reactive = if (isReactor) "Reactor" else "Coroutine"
    val dbClient = if (isSpring) "Spring R2DBC Core DatabaseClient" else "JOOQ"
    val suffix = if (isBefore) "before" else "after"
    logger.info("!!!--- $reactive + $dbClient $suffix")
  }

  private fun findAllCoreQuery(pageable: Pageable) =
    client
      .sql(
        """
        SELECT v.id, v.first_name, v.last_name, s.name as specialty_name, temp.total_count
        FROM vets v
        INNER JOIN (
            SELECT id, count(id) over() as total_count FROM vets ORDER BY id LIMIT :limit OFFSET :offset
        ) temp USING(id)
        LEFT JOIN vet_specialties vs ON v.id = vs.vet_id
        LEFT JOIN specialties s ON vs.specialty_id = s.id
        ORDER BY v.id
        """.trimIndent(),
      ).bind("limit", pageable.pageSize)
      .bind("offset", pageable.offset)
      .fetch()

  private fun map(rows: List<Map<String, Any>>): Pair<Long, List<Vet>> {
    val totalCount = rows.first()["total_count"] as Long
    if (Objects.isNull(totalCount)) {
      return (0L to emptyList())
    }
    return (
      totalCount to
        rows
          .groupBy { it["id"]!! }
          .map { (id, rows) ->
            val specialties =
              rows
                .mapNotNull {
                  it["specialty_name"] as String?
                }.map {
                  Specialty(it)
                }
            val row = rows.first()
            Vet(
              id = id as Int,
              firstName = row["first_name"] as String,
              lastName = row["last_name"] as String,
              specialties = specialties,
            )
          }
    )
  }

  private fun findAllQuery(pageable: Pageable) =
    create
      .select(
        VETS.ID,
        VETS.FIRST_NAME,
        VETS.LAST_NAME,
        multiset(
          selectFrom(VETS.specialties),
        ).intoList { Specialty(it.name) },
        count().over(),
      ).from(VETS)
      .orderBy(VETS.ID.asc())
      .limit(pageable.pageSize)
      .offset(pageable.offset)
}
