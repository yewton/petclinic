package net.yewton.petclinic.owner

// TODO 一旦最低限
data class Pet(var name: String?) {
  override fun toString(): String {
    return name ?: ""
  }
}
