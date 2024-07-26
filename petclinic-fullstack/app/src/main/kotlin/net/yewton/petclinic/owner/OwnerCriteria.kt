package net.yewton.petclinic.owner

/**
 * 参考実装では Owner クラスを使いまわしているけれど、分かりづらいので明確に分けておく
 */
data class OwnerCriteria(var lastName: String? = null)
