import java.io.IOException;
import java.io.RandomAccessFile;

public class DBTable { 
    private RandomAccessFile rows; //the file that stores the rows in the table 
    private long free; //head of the free list space for rows 
    private int numOtherFields; 
    private int otherFieldLengths[];
    //add other instance variables as needed
    private class Row { 
        private int keyField; 
        private char otherFields[][]; 
        /* 
        Each row consists of unique key and one or more character array fields. 
        Each character array field is a fixed length field (for example 10 
        characters). 
        Each field can have a different length. 
        Fields are padded with null characters so a field with a length of 
        of x characters always uses space for x characters. 
        */ 
        //Constructors and other Row methods 

        //this is the contructor for a row in memory
        public Row(int key, char[][] other){
            this.keyField = key; //setting the attributes
            this.otherFields = other;
        }

        //this is the constructor for a row given the address of where that row is stored in DBTable
        public Row(long address) throws IOException{
            rows.seek(address);//seeking the address
            this.keyField = rows.readInt(); //reading the keyField
            for(int i = 0; i<numOtherFields; i++){ //number of fields
                for(int x = 0; x<otherFieldLengths[i]; x++){ //the length of each field
                    otherFields[i][x] = rows.readChar(); //assigning the otherFields attribute
                }
            }
        }

        //this method writes this row to at the long address parameter
        public void writeRow(long address) throws IOException{
            rows.seek(address);
            rows.writeInt(keyField); //first writing keyField
            for(int i = 0; i<numOtherFields; i++){ //number of fields
                for(int x = 0; x<otherFieldLengths[i]; x++){ //length of each field
                    rows.writeChar(otherFields[i][x]); //writing each individual character
                }
            }
        }
    }
    
    public DBTable(String filename, int fL[], int bsize ) throws IOException{ 
        /* 
        Use this constructor to create a new DBTable. 
        filename is the name of the file used to store the table 
        fL is the lengths of the otherFields 
        fL.length indicates how many other fields are part of the row 
        bsize is the block size. It is used to calculate the order of the B+Tree 
        A B+Tree must be created for the key field in the table 
        
        If a file with name filename exists, the file should be deleted before the 
        new file is created. 
        */
        this.rows = new RandomAccessFile(filename, "rw"); //creating the new randomaccessfile
        this.numOtherFields = fL.length;
        this.otherFieldLengths = fL;
        rows.seek(0); //seeking the 0 position to start writing the attributes
        rows.writeInt(numOtherFields);
        for(int i = 0; i<numOtherFields; i++){
            rows.writeInt(otherFieldLengths[i]); //writing each length
        }
        rows.writeLong(-1); //writing the free (-1 because the freelist starts out as empty)
    }

    public DBTable(String filename) throws IOException{ 
        //Use this constructor to open an existing DBTable 
        this.rows = new RandomAccessFile(filename, "rw"); //creating the new RandomAccessFile
        rows.seek(0); //seeking 0 to start reading values to set to attributes
        this.numOtherFields = rows.readInt(); //setting attribute 
        for(int i = 0; i<numOtherFields; i++){
            this.otherFieldLengths[i] = rows.readInt(); //setting the array of row lengths
        }
        this.free = rows.readLong(); //setting the root of the freelist
    }
    
    /*
     * This is a method that takes in the address to the row in the table,
     * and the key k it will be looking for. If the key k is found, true will
     * be returned, else false will be returned
     */
    public boolean searchFor(long address, int k) throws IOException{
        Row cur = new Row(address);
        return cur.keyField == k ? true : false;
    } 

    public boolean insert(int key, char fields[][]) { 
    //PRE: the length of each row is fields matches the expected length 
    /* 
    If a row with the key is not in the table, the row is added and the method 
    returns true otherwise the row is not added and the method returns false. 
    The method must use the B+tree to determine if a row with the key exists. 
    If the row is added the key is also added into the B+tree. 
    */ 
    } 

    public boolean remove(int key) { 
        /* 
        If a row with the key is in the table it is removed and true is returned 
        otherwise false is returned. 
        The method must use the B+Tree to determine if a row with the key exists. 
        
        If the row is deleted the key must be deleted from the B+Tree 
        */ 
    }

    public LinkedList<String> search(int key) { 
        /* 
        If a row with the key is found in the table return a list of the other fields in 
        the row. 
        The string values in the list should not include the null characters. 
        If a row with the key is not found return an empty list 
        The method must use the equality search in B+Tree 
        */
    }

    public LinkedList<LinkedList<String>> rangeSearch(int low, int high) { 
        //PRE: low <= high 
        /* 
        For each row with a key that is in the range low to high inclusive a list 
        of the fields (including the key) in the row is added to the list 
        returned by the call. 
        If there are no rows with a key in the range return an empty list 
        The method must use the range search in B+Tree 
        */ 
    } 

    public void print() { 
        //Print the rows to standard output is ascending order (based on the keys) 
        //Include the key and other fields 
        //print one row per line 
    } 

    public void close() { 
        //close the DBTable. The table should not be used after it is closed 
    } 
}

       
       
       
       