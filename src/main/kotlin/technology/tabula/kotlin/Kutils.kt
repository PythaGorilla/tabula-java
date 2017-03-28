package technology.tabula.kotlin

import technology.tabula.Table

/**
 * Created by Hasee on 2017/3/16.
 */
fun sum(a: Int, b: Int): Int {
    return a + b

}
fun <T> zip2pairs(a:List<T>, b:List<T>) : List<MutableList<T>>
{
    return a.zip(b).map { it.toList().toMutableList()};

}
fun <T> flatZip(a:List<MutableList<T>>, b:List<T>) : List<MutableList<T>>
{
     val newList:MutableList<MutableList<T>> = mutableListOf();
    for(i in a){
        for(j in b){
            i.add(j)
        }

    }

    return a
}
fun <T> zip2map(a:List<T>, b:List<T>): Map<T, List<T>> {
    val list1= a.zip(b)
    val result = list1
            .groupBy { it -> it.first }
            .mapValues { it.value.map { p -> p.second }.toList() }
    return result
}

fun meta2table(meta_map:Map<Int,List<List<String>>>,table_map:Map<Int,List<Table>>): MutableList<Table> {
    val newList:MutableList<Table> = mutableListOf();

    for (page in table_map.keys
            .filter { it in meta_map.keys }) {
        if(page in meta_map.keys) {
            val page_table_num = table_map[page]!!.size
            for (i in 0 until page_table_num) {
                val table = table_map[page]!![i]
                val caption = meta_map[page]!![i]?.get(0)
                val refTxt = meta_map[page]!![i]?.get(1)
                table.setCaption(caption)
                table.setRefTxt(refTxt)
                newList.add(table)
            }
        }}
    for (page in table_map.keys
            .filter {  !(it  in meta_map.keys) }){
            val page_table_num = table_map[page]!!.size
            for (i in 0 until page_table_num) {
                val table = table_map[page]!![i]
                newList.add(table)

    }}

    return newList

}


//fun main(args: Array<String>) {
//    val pairs = listOf(Pair("bob", "UGLY"), Pair("sue", "PETTY"), Pair("bob", "FAT"))
//    val result = pairs
//            .groupBy { it -> it.first }
//            .mapValues { it.value.map { p -> p.second }.toSet() }
//    println(result)
//}
