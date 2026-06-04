package com.client.xvideos.l.model.enum

enum class PictureCountRank(val count: Int){
    All(-1),
    C0_25(0),       //0 to 25
    C25_50(1),      //25 to 50
    C50_100(2),     //50 to 100
    C100_200(3),    //100 to 200
    C200_800(4),    //200 to 800
    C800_3200(5),   //800 to 3200
    C3200_12800(6), //3200 to 12800
}