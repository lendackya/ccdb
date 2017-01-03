package org.jlab.ccdb

import java.sql.DriverManager
import java.sql.Connection

class SQLiteProvider(connectionString:String): JDBCProvider(connectionString) {


    override fun connect()
    {
        //first check for uri type
        val typePos = connectionString.indexOf("sqlite:///")
        if(typePos != 0){
            throw IllegalArgumentException("Connection string doesn't start with sqlite:/// but is given to SQLiteProvider. (Notice 3 slashes ///)")
        }

        //ok we replace CCDB 'sqlite:///' to JDBC 'jdbc:sqlite:'
        val host = "jdbc:sqlite:" + connectionString.substring(10)

        //Connect to through JDBC
        connectJDBC(host)
    }

    private fun connectJDBC(host:String){
        // load the sqlite-JDBC driver using the current class loader
        Class.forName("org.sqlite.JDBC");


        connection = DriverManager.getConnection(host)

        val con:Connection = connection!!

        prsDirectories = con.prepareStatement("SELECT id, parentId, name, created, modified, comment FROM directories");
        prsVariationById = con.prepareStatement("SELECT id, parentId, name FROM variations WHERE id = ?")
        prsVariationByName = con.prepareStatement("SELECT id, parentId, name FROM variations WHERE name = ?")
        //ok now we must build our mighty query...
        val dataQuery=
                "SELECT `assignments`.`id` AS `asId`, "+
                "`constantSets`.`vault` AS `blob`, "+
                "`assignments`.`modified` as `asModified`"+
                "FROM  `assignments` "+
                "INNER JOIN `runRanges` ON `assignments`.`runRangeId`= `runRanges`.`id` "+
                "INNER JOIN `constantSets` ON `assignments`.`constantSetId` = `constantSets`.`id` "+
                "INNER JOIN `typeTables` ON `constantSets`.`constantTypeId` = `typeTables`.`id` "+
                "WHERE  `runRanges`.`runMin` <= ? "+
                "AND `runRanges`.`runMax` >= ? "+
                "AND `assignments`.`variationId`= ? "+
                "AND `constantSets`.`constantTypeId` = ? "

        val timeConstrain = "AND `assignments`.`created` <= datetime(?, 'unixepoch', 'localtime')"
        val orderBy = "ORDER BY `assignments`.`id` DESC LIMIT 1 "
        prsData = con.prepareStatement(dataQuery + timeConstrain + orderBy)

        prsTable = con.prepareStatement("SELECT `id`, `name`, `directoryId`, `nRows`, `nColumns`, `comment` FROM `typeTables` WHERE `name` = ? AND `directoryId` = ?;")
        prsAllTables = con.prepareStatement("SELECT `id`, `name`, `directoryId`, `nRows`, `nColumns`, `comment` FROM `typeTables`")
        prsColumns = con.prepareStatement("SELECT `id`, `name`, `columnType` FROM `columns` WHERE `typeId` = ? ORDER BY `order`;")

        prsVariationIds = con.prepareStatement("SELECT DISTINCT `assignments`.`variationId` AS `varId` FROM `assignments` " +
                "USE INDEX (id_UNIQUE) INNER JOIN `runRanges` ON `assignments`.`runRangeId`= `runRanges`.`id` " +
                "INNER JOIN `constantSets` ON `assignments`.`constantSetId` = `constantSets`.`id` " +
                "INNER JOIN `typeTables` ON `constantSets`.`constantTypeId` = `typeTables`.`id` " +
                "WHERE `constantSets`.`constantTypeId` = ? AND `assignments`.`runRangeId` = ? ORDER BY `assignments`.`created` DESC")

        prsRunRangeIds = con.prepareStatement("SELECT DISTINCT `assignments`.`runRangeId` AS `runId` FROM `assignments` " +
                "USE INDEX (id_UNIQUE) INNER JOIN `runRanges` ON `assignments`.`runRangeId`= `runRanges`.`id` " +
                "INNER JOIN `constantSets` ON `assignments`.`constantSetId` = `constantSets`.`id` " +
                "INNER JOIN `typeTables` ON `constantSets`.`constantTypeId` = `typeTables`.`id` " +
                "WHERE `constantSets`.`constantTypeId` = ?")

        prsVariationsWithRunRangeId = con.prepareStatement("SELECT `assignments`.`id` AS `assignmentId` FROM `assignments` " +
                "USE INDEX (id_UNIQUE) INNER JOIN `runRanges` ON `assignments`.`runRangeId`= `runRanges`.`id` " +
                "INNER JOIN `constantSets` ON `assignments`.`constantSetId` = `constantSets`.`id` " +
                "INNER JOIN `typeTables` ON `constantSets`.`constantTypeId` = `typeTables`.`id` " +
                "WHERE `constantSets`.`constantTypeId` = ? AND `assignments`.`runRangeId` = ? AND `assignments`.`variationId` = ?" +
                " ORDER BY `assignments`.`created` DESC")

        prsRunRangeMax = con.prepareStatement("SELECT `runRanges`.`runMax` AS `runMax` " +
                "FROM `runRanges` WHERE `id` = ?")

        prsRunRangeMin = con.prepareStatement("SELECT `runRanges`.`runMin` AS `runMin` " +
                "FROM `runRanges` WHERE `id` = ?")

        postConnect()
    }
}
