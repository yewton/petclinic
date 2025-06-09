package net.yewton.petclinic.pet

data class PetType(
  val name: String?,
) {
  override fun toString(): String = name ?: ""
}
