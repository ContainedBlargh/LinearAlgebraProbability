class VectorSpace {

    companion object Subspaces {
        fun real(variables: Int): VectorSpace {
            return VectorSpace()
        }
        fun polynomial(variables: Int): VectorSpace {
            return VectorSpace()
        }
    }

    fun isSubsetOf(vectorSpace: VectorSpace): Boolean {
        return false
    }
}

