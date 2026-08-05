package com.client.xvideos.common.json

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

object JsonTypes {

    fun listOf(elementType: Type): Type {
        return ParameterizedTypeImpl(
            rawType = List::class.java,
            typeArguments = arrayOf(elementType)
        )
    }

    private class ParameterizedTypeImpl(
        private val rawType: Type,
        private val ownerType: Type? = null,
        private val typeArguments: Array<Type>
    ) : ParameterizedType {

        override fun getActualTypeArguments(): Array<Type> = typeArguments.clone()

        override fun getRawType(): Type = rawType

        override fun getOwnerType(): Type? = ownerType

        override fun equals(other: Any?): Boolean {
            return other is ParameterizedType &&
                    ownerType == other.ownerType &&
                    rawType == other.rawType &&
                    typeArguments.contentEquals(other.actualTypeArguments)
        }

        override fun hashCode(): Int {
            return typeArguments.contentHashCode() xor rawType.hashCode() xor (ownerType?.hashCode() ?: 0)
        }

        override fun toString(): String {
            return "$rawType<${typeArguments.joinToString(", ")}>"
        }
    }
}
