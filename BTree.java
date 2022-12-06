import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Stack;

public class BTree { 
    private RandomAccessFile f; 
    private int order; 
    private int blockSize; 
    private long root; 
    private long free; 
    //add instance variables as needed. 
    private class BTreeNode { 
        private int count; 
        private int keys[]; 
        private long children[]; 
        private long address; //the address of the node in the file 
        boolean isLeaf; //attribute for if the Node is a leaf or not
        //constructors and other method 

        //constructor for a BTreeNode in memory
        private BTreeNode(){//default constructor initializing variables in memory
            this.count = 0;
            keys = new int[order]; //keys size is one more than it needs to be to help with splitting
            for(int i = 0; i<keys.length; i++){ //initializing values to minvalue
                keys[i] = Integer.MIN_VALUE;
            }
            children = new long[order+1]; //size of children is one more than it needs to be to help with splitting
            //address doesn't need to be initialized yet
            //neither does isleaf
        }
        private BTreeNode(int count, int keys[], long children[], long address){
            this.count = count; //setting all the attributes
            this.keys = keys;
            this.children = children;
            this.address = address;
            isLeaf = count < 0 ? true : false; //count is negative --> is a leaf | count is positive --> not a leaf
        }

        //constructor for a BTreeNode in a file
        private BTreeNode(long address) throws IOException{
            this.keys = new int[order];
            this.children = new long[order+1];

            this.address = address; //setting address attribute
            f.seek(address); //seeking address
            this.count = f.readInt();
            isLeaf = count < 0 ? true : false; //count is negative --> is a leaf | count is positive --> not a leaf
            for(int i = 0; i<keys.length; i++){
                if(i == keys.length-1){
                    keys[i] = Integer.MIN_VALUE;
                    break;
                } 
                keys[i] = f.readInt(); //assigning the keys in the file to memory
            }
            for(int i = 0; i<children.length-1; i++){//order is the number of children
                children[i] = f.readLong(); //assigning the children in the file to memory
                //this assigns everything, even the children that were not given a value
            }
        }

        //method for writing a node to the file at the long address parameter
        private void writeNode(long address) throws IOException{
            f.seek(address);
            f.writeInt(count);
            for(int i = 0; i<keys.length-1; i++){ //order-1 because we are filling the whole block
                if(i < keys.length){ //index within range
                    f.writeInt(keys[i]); //writing the keys to the file
                }else{ //index not within range
                    f.writeInt(Integer.MIN_VALUE); //writing min in the values that are not being used
                }
            }
            for(int i = 0; i<order; i++){
                if(i < children.length){
                    f.writeLong(children[i]);
                }else{
                    f.writeLong(0); //writing min in the values that are not being used
                }
            }
        }
    }

    public BTree(String filename, int bsize) throws IOException { 
        //bsize is the block size. This value is used to calculate the order 
        //of the B+Tree 
        //all B+Tree nodes will use bsize bytes 
        //makes a new B+tree 
        
        //first calculate the order
        this.order = (int) Math.floor(bsize/12); //setting the order attribute
        f = new RandomAccessFile(filename, "rw"); //creating a new randomaccessfile
        //not organizing the attributes in the file
        f.seek(0);
        f.writeLong(-1); //the tree is empty so -1 is written
        f.writeLong(-1); //-1 representing the address of the free list because it is empty
        f.writeInt(bsize); //writing the blocksize 

        this.root = -1;
        this.free = -1;
        this.blockSize = bsize;
        
    } 

    public BTree(String filename) throws IOException { 
        
        //open an existing B+Tree 
        f = new RandomAccessFile(filename, "rw");
        f.seek(0); //seeking to the position of the root
        this.root = f.readLong(); //setting the root attribute
        this.free = f.readLong(); //setting the free attribute
        this.blockSize = f.readInt(); //setting the block size attribute
        this.order = (int) Math.floor(blockSize/12); //setting the order attribute
    }

    public boolean insert(int key, long addr) throws IOException { 
        /* 
        If key is not a duplicate add key to the B+tree 
        addr (in DBTable) is the address of the row that contains the key 
        return true if the key is added 
        return false if the key is a duplicate 
        */
        boolean split = true;
        int[] keysVar = new int[order];

        if(this.root == -1 ){//the tree is empty
            int[] keys = new int[order];
            for(int i = 0; i<keys.length; i++){ //the padding for the arrays is integer.minvalue
                keys[i] = Integer.MIN_VALUE;
            }
            keys = keysArray(key, keys);
            long[] children = new long[order+1]; //nothing in it because there are no children (padded with 0s)
            children[0] = addr; //setting the second child pointer to the correct row of data since key = key
            BTreeNode write = new BTreeNode(-1, keys, children, addr);

            if(isFreeEmpty()){
                write.writeNode(20); //writing the node as the new root and only node 
                f.seek(0);
                f.writeLong(20); //updating root in file
                this.root = 20; //updating root in memory
            }else{//we will write the node in the freelist
 //               long address = addToFree(write); fix this when we get to remove
                f.seek(0);
//                f.writeLong(address); 
//                  Also this is the case for the root so that must be updated in memory and in the file
            }
            return true; 
        }else{
            //starting with searching for the position where we will do an insert
            Stack<BTreeNode> stack = searchWithStack(this.root, key);

            BTreeNode cur = stack.pop(); //this is the leaf node we will look at to start

            keysVar = keysArray(key,cur.keys); //this will add the key to our node
            if(keysVar != null){ //if it is null that would indicate there isnt room
                cur.keys = keysVar;
                split = false; //setting split to false since we wont be splitting the node
                int childIndex = compareForChildren(key, cur.keys); //this is the index of where the address will go
                //shift over childrenarray
                cur.children = shiftChildren(childIndex, cur.children);
                cur.children[childIndex] = addr; //setting the address
                cur.count = getCount(cur.keys, true); //this is the negation of it because we are at a leaf
                cur.writeNode(cur.address); //writing the node to the file where it relies
            }else{//else the node is full 
                BTreeNode newNode = split(cur, key, addr); //this method will return the right values node and modify cur to be the left
                int[] x = cur.keys;


            }


            return false;
        }
    }

    /*
     * This method returns a BTreeNode. It returns the right values node and modifies the parameter node to be the left
     * it also takes the key as a parameter along with the long address to be added to children 
     */
    public BTreeNode split(BTreeNode left, int key, long address){
        BTreeNode right = new BTreeNode();
        //creating the key arrays for the nodes
        left.keys = keysNotNullArray(key, left.keys); //this adds the key to the array;

        int index = compareForChildren(key, left.keys); //this is the index of where we will add to the children array
        left.children = shiftChildren(index,left.children); //shifting the children
        left.children[index] = address; //adding the new address

        right.keys = splitValuesRight(left.keys); //splitting the keys now
        left.keys = splitValuesLeft(left.keys);
        //the keys of both are split
        
        



        return right;

    }

    /*
     * Returns an array of the left keys
     */
    public int[] splitValuesLeft(int[] arr){
        int[] ret = new int[order];
        for(int i = 0; i<ret.length; i++){
            if(i < Math.floor(ret.length/2)){
                ret[i] = arr[i];
            }
            else{
                ret[i] = Integer.MIN_VALUE;
            }
        }
        return ret;
    }

    /*
     * Returns an array of the right keys
     */
    public int[] splitValuesRight(int[] arr){
        int[] ret = new int[order];
        for(int i = 0; i<ret.length; i++){
            if(i >= Math.ceil(ret.length/2)){
                ret[i] = arr[i];
            }else{
                ret[i] = Integer.MAX_VALUE;
            }
        }
        return ret;
    }

    /*
     * Returns an array of the left children pointers 
     * parameter address is the new address associated with the new key
     */
//    public long[] splitValuesLeft(long[] arr){

//    }

    /*
     * Returns an array of the right children pointers
     * parameter address is the new address associated with the new key
     */
//    public long[] splitValuesRight(long[] arr){

//    }

    /*
     * This method shifts the children over by one so that an insert can be made
     * PRE: children cannot be full
     */
    public long[] shiftChildren(int index, long[] children){
        long temp = 0;
        long t;
        for(int i = index; i<children.length-1; i++){
            if(i == index){
                temp = children[i+1];
                children[i+1] = children[i];
            }else{
                t = children[i+1];
                children[i+1] = temp;
                temp = t;
                
            }
        }
        return children;
    }

    public long[] shiftChildrenSplit(int index, long[] children){
        long temp = 0;
        long t;
        for(int i = index; i<children.length-1; i++){
            if(i == index){
                temp = children[i+1];
                children[i+1] = children[i];
            }else{
                t = children[i+1];
                children[i+1] = temp;
                temp = t;
                
            }
        }
        return children;
    }

    /*
     * This method assumes you are adding one to the count,
     * it counts the number of keys and then returns that plus 1
     */
    public int getCount(int[] keys, boolean leaf){
        int x = 0;
        for(int i = 0; i<keys.length-1; i++){
            if(keys[i] != Integer.MIN_VALUE && !leaf) x++;
            if(keys[i] != Integer.MIN_VALUE && leaf) x--;
        }
        return x;
    }
    

    /*
     * This is a method that puts the search path into a stack
     * and returns that stack
     */
    public Stack<BTreeNode> searchWithStack(long address, int k) throws IOException{ //address is the starting point of the search (root) and k is the key
        BTreeNode cur = new BTreeNode(address);
        Stack<BTreeNode> ret = new Stack<BTreeNode>();
        ret.push(cur); //pushing cur to the stack as it is the first node in the search path
        while(!cur.isLeaf){
            long nextAddress = compareForChildren(k, cur.keys);
            cur = new BTreeNode(nextAddress);
            ret.push(cur); //pushing the next node in the search path onto the stack
        }
        return ret;
    }

    /*
     * This method returns true if there is room for another key in the BTreeNode
     * this means we are in one of the simplest cases of insert
     */

    /*
     * This is a method that returns true if the freelist is empty
     * and false if the freelist is not empty
     */
    public boolean isFreeEmpty() throws IOException{
        f.seek(8); //address of freelist
        long freeAddress = f.readInt();
        if(freeAddress == -1){
            return true;
        }else{
            return false;
        }
    }

    /*
     * This is a method that takes a node parameter and writes it to the first position in the freelist
     * it then makes the following position in the freelist the head of the freelist
     * It also returns the position in the file where the node was written at
     */

/*    public long addToFree(BTreeNode add){
        
    }*/


    /*
     * This is a method that adds the integer we want to a the sorted array
     * The sorted array is always padded with 0s
     */
    public int[] keysArray(int add, int[] curArray){// add is the int we will add to the array and curArray is the array we will add it to
        if(curArray[0] == Integer.MIN_VALUE){
            curArray[0] = add;
            return curArray;
        }
        int n = 0;
        for(int i = 0; i<curArray.length; i++){
            if(curArray[i] != Integer.MIN_VALUE) n++;
        }
        if(n>=order-1) return null; //returning null because the array is full
        int i;
        for(i = n - 1; (i>= 0 && curArray[i] > add); i--){
            curArray[i+1] = curArray[i];
        }
        curArray[i+1] = add;
        return curArray;
    }



    public int[] keysNotNullArray(int add, int[] curArray){// add is the int we will add to the array and curArray is the array we will add it to
        if(curArray[0] == Integer.MIN_VALUE){
            curArray[0] = add;
            return curArray;
        }
        int n = 0;
        for(int i = 0; i<curArray.length; i++){
            if(curArray[i] != Integer.MIN_VALUE) n++;
        }
        int i;
        for(i = n - 1; (i>= 0 && curArray[i] > add); i--){
            curArray[i+1] = curArray[i];
        }
        curArray[i+1] = add;
        return curArray;
    }


/* 
    public long remove(int key) { 
        /* 
        If the key is in the Btree, remove the key and return the address of the 
        row 
        return 0 if the key is not found in the B+tree 
        
    } 
*/
       //this is where i am at now
    public long search(int k) throws IOException { 
        /* 
        This is an equality search 
        If the key is not found return the address of the row with the key 
        otherwise return 0 
        */ 
        BTreeNode cur = new BTreeNode(this.root); //this is the root node
        while(!cur.isLeaf){
            int numChild = compareForChildren(k, cur.keys); //this will return us the index we need to access the child we are looking for
            cur = new BTreeNode(cur.children[numChild]);
        }//when we exit the while loop cur is a leaf that may contain what we are looking for
        //use searchFor function in dbtable to return true if it was found and false if it wasnt
        long dbTableAddress = cur.children[compareForChildren(k, cur.keys)]; //this is now the address of the row
        for(int i = 0; i<cur.keys.length-1; i++){
            if(cur.keys[i] == k){
                //if it was found, then we return the address to the blo
                return 0;
            }
        } 
        //otherwise it was not so return 0
        return dbTableAddress;

    } 

    /*
     * This is a method that returns the index that indicates range
     * This index will be used in the children array to traverse through
     * the B+ tree
     */
    public int compareForChildren(int key, int[] keyArr){
        for(int i = 0; i<keyArr.length; i++){
            if(keyArr[i] == key) return i;
        }
        return 0;
    }



/* 
    public LinkedList<Long> rangeSearch(int low, int high){
        //PRE: low <= high 
        /* 

        return a list of row addresses for all keys in the range low to high inclusive 
        the implementation must use the fact that the leaves are linked 
        return an empty list when no keys are in the range 
        
    }
*/

    public void print() { 
        //print the B+Tree to standard output 
        //print one node per line 
        //This method can be helpful for debugging 
    }
    
    public void printRoot() throws IOException{
        f.seek(this.root);
        int content = f.readInt();
        System.out.println("This is the content: "+content);
        System.out.print("These are the keys: ");
        for(int i = 0; i<4; i++){
            System.out.print(f.readInt()+" ");
        }
        System.out.println();
        System.out.print("These are the children: ");
        for(int i = 0; i<5; i++){
            System.out.print(f.readLong()+" ");
        }
        System.out.println();

    }

    public void close() { 
        //close the B+tree. The tree should not be accessed after close is called 
    }
    
    public static void main(String[] args) throws IOException{
        BTree tree = new BTree("./BtreeFile", 60);
        tree.insert(5, 500);
        tree.insert(8, 550); //fix the issues with this
        tree.insert(9, 200);
        tree.insert(6, 5123);
        tree.insert(7, 123);
        tree.printRoot();
    }
}


