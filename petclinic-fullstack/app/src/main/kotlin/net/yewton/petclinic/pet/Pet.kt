package net.yewton.petclinic.pet

import jakarta.validation.constraints.NotEmpty
import net.yewton.petclinic.owner.Owner
import net.yewton.petclinic.visit.Visit
import java.time.LocalDate

data class Pet(
  var id: Int?,
  @field:NotEmpty
  var name: String?,
  var birthDate: LocalDate?,
  var type: PetType,
  val visits: MutableSet<Visit> = hashSetOf(),
) {
  override fun toString(): String = name ?: ""

  fun isNew(): Boolean = id == null

  fun addOwner(owner: Owner) {
    owner.addPet(this)
  }
}
