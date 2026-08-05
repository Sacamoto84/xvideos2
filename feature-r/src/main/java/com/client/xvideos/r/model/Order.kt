package com.client.xvideos.r.model

enum class Order(val value: String) {
    TRENDING("trending"),
    TOP("top"),
    LATEST("latest"),
    OLDEST("oldest"),
    RECENT("recent"),
    BEST("best"),
    TOP28("top28"),
    
    //NEW("new"),

    FORCE_TEMP(""),

    TOP_WEEK("week"),
    TOP_MONTH("month"),
    //TOP ALL TIME
    TOP_ALLTIME("alltime"),


    //NICHES
    NICHES_SUBSCRIBERS_D("subscribers"),

    NICHES_SUBSCRIBERS_A("subscribers"),

    NICHES_POST_D("posts"),

    NICHES_POST_A("posts"),
    NICHES_NAME_A_Z("name"),
    NICHES_NAME_Z_A("name"),

}

enum class MediaType(val value: String) {
    IMAGE("i"),
    GIF("g"),
    ALL("all")
}











