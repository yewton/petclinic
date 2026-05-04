package net.yewton.petclinic.pet

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class PetTypeConverter : Converter<String, PetType> {
  override fun convert(source: String): PetType = PetType(name = source)
}
