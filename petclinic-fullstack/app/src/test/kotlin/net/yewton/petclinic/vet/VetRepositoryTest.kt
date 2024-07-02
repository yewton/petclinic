package net.yewton.petclinic.vet

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest

@SpringBootTest
class VetRepositoryTest : WithAssertions {

    @Autowired
    lateinit var vetRepository: VetRepository

    @Test
    fun myTest() = runTest {
        val result = vetRepository.findAll(PageRequest.of(0, 5))
        assertThat(result.size).isEqualTo(5)
        assertThat(result.content.map { ("${it.firstName} ${it.lastName}" to it.specialties?.map { sp -> sp.name }) })
            .isEqualTo(
                listOf(
                    "James Carter" to listOf(),
                    "Helen Leary" to listOf("radiology"),
                    "Linda Douglas" to listOf("surgery", "dentistry"),
                    "Rafael Ortega" to listOf("surgery"),
                    "Henry Stevens" to listOf("radiology")
                )
            )
    }
}
