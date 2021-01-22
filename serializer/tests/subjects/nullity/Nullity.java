class Nullity {
    Nullity next;

    public boolean hasMoreElements() {
        /**
         * <expected>
         * [this (ctx-lookup ctx "this")
         *  peg  (rd this "next" heap)
         *  (snapshot {:heap   heap
         *             :return (opnode "!=" peg (null-lit))})
         *  ]
         * </expected>
         */
        return next!=null;
    }
}
