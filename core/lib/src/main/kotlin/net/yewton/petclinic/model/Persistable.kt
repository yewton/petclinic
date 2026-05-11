package net.yewton.petclinic.model

interface Persistable<ID> {
  val id: ID?

  fun isNew(): Boolean = id == null
}
