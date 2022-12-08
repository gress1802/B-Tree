import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedList;

public class DBTable { 
    private RandomAccessFile rows; //the file that stores the rows in the table 
    private long free; //head of the free list space for rows 
    private int numOtherFields; 
    private int otherFieldLengths[];
    private BTree tree;
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
            otherFields = new char[numOtherFields][];
            for(int i = 0; i < otherFieldLengths.length; i++){
                otherFields = new char[numOtherFields][otherFieldLengths[i]];
            }
            rows.seek(address);//seeking the address
            this.keyField = rows.readInt(); //reading the keyField
            for(int i = 0; i<numOtherFields; i++){ //number of fields
                for(int x = 0; x<otherFieldLengths[i]; x++){ //the length of each field
                    char y = rows.readChar();
                    if(y=='\0'){
                    }else{
                        otherFields[i][x] = y; //assigning the otherFields attribute
                    }
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
        tree = new BTree(filename+"BTREE", bsize);
        this.rows = new RandomAccessFile(filename, "rw"); //creating the new randomaccessfile
        rows.setLength(0); //reseting file
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
        tree = new BTree(filename+"BTREE"); //initializing the new BTree
        //Use this constructor to open an existing DBTable 
        this.rows = new RandomAccessFile(filename, "rw"); //creating the new RandomAccessFile
        rows.seek(0); //seeking 0 to start reading values to set to attributes
        this.numOtherFields = rows.readInt(); //setting attribute
        otherFieldLengths = new int[numOtherFields]; 
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

    public boolean insert(int key, char fields[][]) throws IOException { 
    //PRE: the length of each row is fields matches the expected length 
    /* 
    If a row with the key is not in the table, the row is added and the method 
    returns true otherwise the row is not added and the method returns false. 
    The method must use the B+tree to determine if a row with the key exists. 
    If the row is added the key is also added into the B+tree. 
    */ 
        long address = rows.length();
        if(!tree.insert(key, address)) return false;// if the insertion was unsuccessful return false
        //else we inserted the key into the BTree
        Row newRow = new Row(key, fields); //create the new row
        newRow.writeRow(address); //write the new row
        return true;
    } 

//    public boolean remove(int key) { 
        /* 
        If a row with the key is in the table it is removed and true is returned 
        otherwise false is returned. 
        The method must use the B+Tree to determine if a row with the key exists. 
        
        If the row is deleted the key must be deleted from the B+Tree 
        */ 
//    }

    public LinkedList<String> search(int key) throws IOException { 
        /* 
        If a row with the key is found in the table return a list of the other fields in 
        the row. 
        The string values in the list should not include the null characters. 
        If a row with the key is not found return an empty list 
        The method must use the equality search in B+Tree 
        */
        long rowAddress = tree.search(key);
        LinkedList<String> ret = new LinkedList<String>();
        if(rowAddress == 0) return ret;
        Row currentRow = new Row(rowAddress);
        ret.add(Integer.toString(currentRow.keyField));
        char[][] fields = currentRow.otherFields;
        for(int i = 0; i<fields.length; i++){
            String thisString = "";
            for(int x = 0; x<fields[i].length; x++){
                thisString = thisString+fields[i][x];
                if(fields[i][x] == '\0') break;
            }
            ret.add(thisString);
        }
        return ret;
    }

    public LinkedList<LinkedList<String>> rangeSearch(int low, int high) throws IOException { 
        //PRE: low <= high 
        /* 
        For each row with a key that is in the range low to high inclusive a list 
        of the fields (including the key) in the row is added to the list 
        returned by the call. 
        If there are no rows with a key in the range return an empty list 
        The method must use the range search in B+Tree 
        */ 
        LinkedList<Long> list = tree.rangeSearch(low, high);
        LinkedList<LinkedList<String>> ret = new LinkedList<LinkedList<String>>();
        for(int i = 0; i<list.size(); i++){
            LinkedList<String> currentList = new LinkedList<String>();
            long address = list.get(i); //getting the otherfields 
            Row cur = new Row(address);
            currentList.add(Integer.toString(cur.keyField));
            char[][] fields = cur.otherFields;

            //parsing the otherfields to strings
            for(int x = 0; x<fields.length; x++){
                String thisString = "";
                for(int z = 0; z<fields[x].length; z++){
                    thisString = thisString+fields[x][z];
                    if(fields[x][z] == '\0') break;
                }
                currentList.add(thisString);
            }
            ret.add(currentList);
        }
        return ret;
    } 

    public void print() throws IOException { 
        //Print the rows to standard output is ascending order (based on the keys) 
        //Include the key and other fields 
        //print one row per line
        LinkedList<Long> range = tree.rangeSearch(Integer.MIN_VALUE, Integer.MAX_VALUE);
        for(int i = 0; i<range.size(); i++){ //go through each row
            Row curRow = new Row(range.get(i));
            System.out.print("Key: "+curRow.keyField+" ");
            for(int x = 0; x<curRow.otherFields.length; x++){
                System.out.print("Otherfield "+(x+1)+": ");
                for(int z = 0; z<curRow.otherFields[x].length;z++){
                    System.out.print(curRow.otherFields[x][z]);
                    if(curRow.otherFields[x][z] == '\0') break;
                }
            }
            System.out.println(); //newline
        }
        
    } 

    public void printTree() throws IOException{
        tree.print();
    }

    public void close() throws IOException { 
        //close the DBTable. The table should not be used after it is closed
        rows.seek(0);
        rows.writeInt(numOtherFields);
        for(int i = 0; i<numOtherFields; i++){
            rows.writeInt(otherFieldLengths[i]);
        } 
        rows.writeLong(free);
        rows.close();
    } 

    public void printFirstRow() throws IOException{
        rows.seek(20);
        System.out.println(rows.readInt());
        System.out.println(rows.readChar());
    }

    public static void main(String[] args) throws IOException{
        //this test creates an DBTable with a height 1 order 5 BTree
        //and inserts, removes, print and reuses the tree
        System.out.println("Start test 3");
        int i;
        int sFieldLens[] = {10, 15};
        int nums[] = {9, 5, 1, 13, 17, 2, 6, 7, 8, 3, 4, 10, 18, 11, 12, 14, 19, 15, 16, 20};
        int len = nums.length;
        DBTable t3 = new DBTable("t3", sFieldLens, 60);
        char sFields[][] = new char[2][];
        for ( i = 0; i < len; i++) {
            sFields[0] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 10);
            sFields[1] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 15);

            t3.insert(nums[i], sFields);
        }
        
        System.out.println("Past inserts in test 3");
        
        /*for (i = len-1; i > 4; i--) {
            t3.remove(nums[i]);
        }
        System.out.println("Print after removes in test 3");
        t3.print();*/

        t3.close();
        
        t3 = new DBTable("t3");
        System.out.println("Print after reuse in test 3");
        t3.print();
        t3.close();
    }
}

       
       
       
       