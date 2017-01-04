/**
 * Created by Dmitry on 3/24/2014.
 */


package org.jlab.ccdb

import com.sun.javafx.scene.shape.PathUtils
import org.jlab.ccdb.helpers.combinePath
import kotlin.properties.Delegates
import kotlin.text.toBoolean
import org.jlab.ccdb.JDBCProvider
import java.security.Timestamp
import java.util.*

val dataSeparator = '|'

class Directory(
        val id:Int,             /// DB id
        val parentId:Int,       /// DB id of parent directory. Id=0 - root directory
        val name:String,        /// Name of the directory
        val createdTime:Date,   /// Creation time
        val modifiedTime:Date,  /// Last modification time
        val comment:String      /// Full description of the directory
        ){

    var parentDirectory:Directory?=null     /// null if there is no parent directory
    var fullPath = ""                       /// Full path (including self name) of the directory


    var subdirectories = Vector<Directory>()


    /**
     * @brief Adds a subdirectory of this directory
     *
     * Adds a subdirectory of this directory
     * Automatically adds "this" as mParent for child
     *
     * @param subDirectory Child directory to be added
     */
    fun addSubdirectory(subdirectory:Directory ){
        subdirectory.parentDirectory = this;
        subdirectories.add(subdirectory)

    }

    /**
     * @brief deletes all subdirectories recursively
     */
    fun disposeSubdirectories(){
        //TODO do we need it with garbage collector???
        if (subdirectories.size >0 ){
            for(subdirectory in subdirectories){
                subdirectory.disposeSubdirectories()
            }
            subdirectories.clear()
        }
    }
}


class Variation(
        val id:Int,
        val parentId:Int,
        val name:String){

    var parentVariation:Variation? = null
    var children = Vector<Variation>()

    fun setParent(parent:Variation){
        this.parentVariation = parent
        parent.children.add(this)
    }
}

public class TypeTable(
        public val id:Int,
        public val directory:Directory,
        public val name:String,
        public val columns: Vector<TypeTableColumn>,
        public val rowsCount:Int)
{
    public val fullPath: String
        get(){
                return combinePath(directory.fullPath, name)
        }

    private var isDoneColumnsByName=false

    public val _columnsByName: HashMap<String, TypeTableColumn> = HashMap<String, TypeTableColumn>()
    public val columnsByName: HashMap<String, TypeTableColumn>
        get(){
            if(!isDoneColumnsByName){
                for(column in columns) _columnsByName[column.name]=column
                isDoneColumnsByName = false
            }
            return _columnsByName
        }
}

class TypeTableColumn(
        val id:Int,
        val name:String,
        val index:Int,
        val cellType:CellTypes)


/**
 * Class that represents CCDB data set
 */
class Assignment(
        val id:Int,
        val blob:String,
        val typeTable:TypeTable,
        val created:Date,
        val variation:Variation,
        val run:Int
){
    /**
     * Gets number of rows
     */
    public val rowCount:Int get(){ return typeTable.rowsCount}


    /**
     * Gets number of columns
     */
    public val columnCount:Int get(){return typeTable.columns.size}

    /**
     * Gets data represented as one vector of string values
     */
    private  val _vectorString: Vector<String> = Vector<String>()
    public val vectorString: Vector<String>
    get(){
        if(_vectorString.isEmpty()){
            for (token in blob.split(dataSeparator)) _vectorString.add(token)
        }
        return _vectorString
    }

    /**
     * Gets data represented as one vector of int values
     */
    public val vectorInt: Vector<Int> by lazy {
        val result = Vector<Int>()
        for (token in blob.split(dataSeparator)) result.add(token.toInt())
        result
    }

    /**
     * Gets data represented as one vector of int values
     */
    public val vectorLong: Vector<Long> by lazy {
        val result = Vector<Long>()
        for (token in blob.split(dataSeparator)) result.add(token.toLong())
        result
    }

    /**
     * Gets data represented as one vector of int values
     */
    public val vectorDouble: Vector<Double> by lazy {
        val result = Vector<Double>()
        for (token in blob.split(dataSeparator)) result.add(token.toDouble())
        result
    }

    /**
     * Gets data represented as one vector of boolean values
     */
    public val vectorBoolean: Vector<Boolean> by lazy {
        val result = Vector<Boolean>()
        for (token in blob.split(dataSeparator)) result.add(token.toBoolean())
        result
    }


    /**
     * Gets data represented as row-wise table
     */
    private val _tableString = Vector<Vector<String>>()
    public val tableString:Vector<Vector<String>>
        get(){
            if(_tableString.isEmpty()){
                val ncols = typeTable.columns.size
                val nrows = typeTable.rowsCount

                for(rowIndex in 0..nrows-1){
                    val row = Vector<String>()
                    for(colIndex in 0..ncols-1){
                        row.add(vectorString[rowIndex*ncols + colIndex])
                    }
                    _tableString.add(row)
                }
            }
            return _tableString
        }

    /**
     * Gets data represented as row-wise table of Integers
     */
    public val tableInt:Vector<Vector<Int>> by lazy {
        val result = Vector<Vector<Int>>()
        for(row in tableString){
            val parsedRow = Vector<Int>()
            for(cell in row){
                parsedRow.add(cell.toInt())
            }
            result.add(parsedRow)
        }
        result
    }

    /**
     * Gets data represented as row-wise table of Longs
     */
    public val tableLong:Vector<Vector<Long>> by lazy {
        val result = Vector<Vector<Long>>()
        for(row in tableString){
            val parsedRow = Vector<Long>()
            for(cell in row){
                parsedRow.add(cell.toLong())
            }
            result.add(parsedRow)
        }
        result
    }

    /**
     * Gets data represented as row-wise table of Doubles
     */
    public val tableDouble:Vector<Vector<Double>> by lazy {
        val result = Vector<Vector<Double>>()
        for(row in tableString){
            val parsedRow = Vector<Double>()
            for(cell in row){
                parsedRow.add(cell.toDouble())
            }
            result.add(parsedRow)
        }
        result
    }

    /**
     * Gets data represented as row-wise table of Booleans
     */
    public val tableBoolean:Vector<Vector<Boolean>> by lazy {
        val result = Vector<Vector<Boolean>>()
        for(row in tableString){
            val parsedRow = Vector<Boolean>()
            for(cell in row){
                parsedRow.add(cell.toBoolean())
            }
            result.add(parsedRow)
        }
        result
    }


    /**
     * gets data represented as map of {column name: data}
     */
    private val _mapString = HashMap<String, String>()
    public val mapString: HashMap<String, String>
        get(){
            if(_mapString.isEmpty()) {
                val ncols = typeTable.columns.size

                for (colIndex in 0..ncols - 1) {
                    _mapString[typeTable.columns[colIndex].name] = vectorString[colIndex]
                }
            }
            return _mapString
        }

    /**
     * Gets all values in one column by column name
     */
    public fun getColumnValuesString(columnName:String): Vector<String>{
        val column = typeTable.columnsByName[columnName]!!;
        return getColumnValuesString(column.index)
    }

    /**
     * Gets all values in one column by column name
     */
    public fun getColumnValuesInt(columnName:String): Vector<Int>{
        val column = typeTable.columnsByName[columnName]!!;
        return getColumnValuesInt(column.index)
    }

    /**
     * Gets all values in one column by column name
     */
    public fun getColumnValuesLong(columnName:String): Vector<Long>{
        val column = typeTable.columnsByName[columnName]!!;
        return getColumnValuesLong(column.index)
    }

    /**
     * Gets all values in one column by column name
     */
    public fun getColumnValuesDouble(columnName:String): Vector<Double>{
        val column = typeTable.columnsByName[columnName]!!;
        return getColumnValuesDouble(column.index)
    }

    /**
     * Gets all values in one column by column name
     */
    public fun getColumnValuesBoolean(columnName:String): Vector<Boolean>{
        val column = typeTable.columnsByName[columnName]!!;
        return getColumnValuesBoolean(column.index)
    }

    /**
     * Gets all values in one column by column index
     */
    public fun getColumnValuesString(columnIndex:Int): Vector<String>{
        val result = Vector<String>()
        for(rowIndex in 0 .. rowCount - 1){
            result.add(vectorString[rowIndex*columnCount + columnIndex])
        }
        return result;
    }

    /**
     * Gets all values as Int in one column by column index
     */
    public fun getColumnValuesInt(columnIndex:Int): Vector<Int>{
        val result = Vector<Int>()
        val values = getColumnValuesString(columnIndex)
        for(value in values){
            result.add(value.toInt())
        }
        return result;
    }

    /**
     * Gets all values as Double in one column by column index
     */
    public fun getColumnValuesDouble(columnIndex:Int): Vector<Double>{
        val result = Vector<Double>()
        val values = getColumnValuesString(columnIndex)
        for(value in values){
            result.add(value.toDouble())
        }
        return result;
    }

    /**
     * Gets all values as Long in one column by column index
     */
    public fun getColumnValuesLong(columnIndex:Int): Vector<Long>{
        val result = Vector<Long>()
        val values = getColumnValuesString(columnIndex)
        for(value in values){
            result.add(value.toLong())
        }
        return result;
    }

    /**
     * Gets all values as Boolean in one column by column index
     */
    public fun getColumnValuesBoolean(columnIndex:Int): Vector<Boolean>{
        val result = Vector<Boolean>()
        val values = getColumnValuesString(columnIndex)
        for(value in values){
            result.add(value.toBoolean())
        }
        return result;
    }

}

/**
 *
 * types from 'int', 'uint', 'long', 'ulong', 'double', 'string', 'bool'
 */
public enum class CellTypes{
    BOOL,
    INT,
    UINT,
    LONG,
    ULONG,
    DOUBLE,
    STRING;

    override fun toString():String{
        return when(this){
            INT -> "int"
            UINT -> "uint"
            LONG -> "long"
            ULONG -> "ulong"
            DOUBLE -> "double"
            STRING -> "string"
            BOOL -> "bool"
        }
    }
}

/**
 * Created by Andrew Lendacky on 12/21/16.
 */
class ConstantsEntry( private val provider:JDBCProvider){

    var runMin: Int = 0
    var runMax: Int = 0
    lateinit var variation: String
    lateinit var parentVariation:String

    lateinit var time:Date

    /**
     * Gets all variations entries for table at all runs, and all variations.
     *
     * @param table the table path name
     *
     * @return returns a Vector containing the entries for the given table.
     */
     public fun getEntries(table:String):LinkedList<ConstantsEntry>{

        var entries:LinkedList<ConstantsEntry> = LinkedList<ConstantsEntry>()

        // is the user connected?
        if (this.provider.isConnected){
            // returns full list of assignments for given table, for all runs and all variations
            entries = this.provider.getConstantEntries(table)

            return entries
        }else{

            print("Provider is not connected.\n")
            return entries // will return empty if not connected
        }
    }


    public fun filterEntriesBy(table:String, variation:String, run:Int):LinkedList<ConstantsEntry>{

        val entries:LinkedList<ConstantsEntry> = this.provider.getConstantEntries(table)
        val sortedEntries:LinkedList<ConstantsEntry> = LinkedList<ConstantsEntry>()

        for (entry in entries){

            if (entry.variation == variation && run <= entry.runMax && run >= entry.runMin){
                sortedEntries.add(entry)
            }
        }

        return sortedEntries
    }


    public fun filterEntriesBy(table:String, run: Int):LinkedList<LinkedList<ConstantsEntry>>{

        val entries:LinkedList<ConstantsEntry> = this.provider.getConstantEntries(table)
        val filteredEntries:LinkedList<LinkedList<ConstantsEntry>> = LinkedList<LinkedList<ConstantsEntry>>()
        val variationNames:HashSet<String> = HashSet<String>() // keeps the name of all distinct variation names in entries
        val filtered:LinkedList<ConstantsEntry> = LinkedList<ConstantsEntry>()

        // get the entries where the run falls within the range
        for (entry in entries){
            if (run >= entry.runMin && run < entry.runMax){ filtered.add(entry) }
        }

        // get each distinct variation name from the filtered list
        for (entry in filtered){
            variationNames.add(entry.variation)
        }

        // create a list for each variation and add it to filteredEntries
        for (name in variationNames){

            val variation:LinkedList<ConstantsEntry> = LinkedList<ConstantsEntry>()

            for (entry in filtered){

                if (entry.variation == name){ variation.add(entry) }
            }

            filteredEntries.add(variation)
        }

        return filteredEntries
    }


    public fun filterEntriesBy(table:String, variation: String):LinkedList<LinkedList<ConstantsEntry>>{

        val entries:LinkedList<ConstantsEntry> = this.provider.getConstantEntries(table)
        val filteredEntries:LinkedList<LinkedList<ConstantsEntry>> = LinkedList<LinkedList<ConstantsEntry>>()
        val filtered:LinkedList<ConstantsEntry> = LinkedList<ConstantsEntry>()


        val mins:HashSet<Int> = HashSet<Int>()
        val maxs:HashSet<Int> = HashSet<Int>()

        // get the entries where the run falls within the range
        for (entry in entries){
            if (entry.variation == variation){ filtered.add(entry) }
        }

        // get each distinct variation name from the filtered list
        for (entry in filtered){
            mins.add(entry.runMin)
            maxs.add(entry.runMax)
        }

        val numDistinctRuns = mins.size

        for (i in 0..numDistinctRuns - 1){

           var ranges:LinkedList<ConstantsEntry> = LinkedList<ConstantsEntry>()

            for(entry in filtered){

                if (entry.runMin == mins.elementAt(i) && entry.runMax == maxs.elementAt(i)){ ranges.add(entry) }
            }

            filteredEntries.add(ranges)
        }

        return filteredEntries

    }

//    private fun getDistinctRunRanges(runRanges: HashSet<RunRange>):HashSet<RunRange>{
//
//
//        for (runRange in runRanges){
//
//
//
//        }
//
//    }


    /**
     * Gets all variations entries for table applying to the given run.
     *
     * @param table the table path name
     * @param run the run of the table
     *
     * @return returns a Vector containing the entries for the given table and run.
     */
    public fun getEntries(table:String, run:Int):LinkedList<ConstantsEntry>{

        var entries:LinkedList<ConstantsEntry>
        val runEntries:LinkedList<ConstantsEntry> = LinkedList<ConstantsEntry>() // holds the entries within a given run

        // is the user connected?
        if (this.provider.isConnected){
            //returns all variation entries for the table that will apply to this run
            entries = this.getEntries(table)

            // check if the run falls within the run range
            for (entry in entries){
                if (run <= entry.runMax && run >= entry.runMin){ runEntries.add(entry) }
            }

            return runEntries
        }else{

            print("Provider is not connected.\n")
            return runEntries // will return empty if not connected
        }
    }

    public fun printConstantEntryInfo(){

        println("Name: " + this.variation)
        //println("Parent Variation: " + this.parentVariation)
        println("Run Min: " + this.runMin)
        println("Run Max: " + this.runMax)
    }


    class RunRange(val min:Int,
                   val max:Int){



    }
}