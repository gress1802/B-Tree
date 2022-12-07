import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
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
        f.setLength(0); //setting the file size to 0 because we are creating a new file
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
        //first check if the key is already in the tree
        if(this.root != -1 && isInTree(key)) return false;
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
            split = false;

        }else{
            //starting with searching for the position where we will do an insert
            Stack<BTreeNode> path = searchWithStack(this.root, key);

            BTreeNode cur = path.pop(); //this is the leaf node we will look at to start

            keysVar = keysArray(key,cur.keys); //this will add the key to our node
            if(keysVar != null){ //if it is null that would indicate there isnt room
                cur.keys = keysVar;
                split = false; //setting split to false since we wont be splitting the node
                int childIndex = compareForChildren(key, cur.keys); //this is the index of where the address will go
                //shift over childrenarray
                cur.children = shiftChildrenLeaves(childIndex, cur.children);
                cur.children[childIndex] = addr; //setting the address
                cur.count = getCount(cur.keys, cur.isLeaf); //this is the negation of it because we are at a leaf
                cur.writeNode(cur.address); //writing the node to the file where it relies
            }else{//else the node is full 
                BTreeNode newNode = split(cur, key, addr, true); //this method will return the right values node (newNode) and modify cur to be the left(node)
                //newNode is now the right values node while cur is the left values node
                cur.children[order-1] = f.length(); //change this when implementing freelist
                int val = newNode.keys[0];//this is the smallest value

                //setting pointers at the leaves


                cur.writeNode(cur.address); //updating cur (the left node)

                //now adding newnode to the file (freelist or f.length)
                long loc = 0; //this is the address in the file of newNode
                if(isFreeEmpty()){
                    loc = f.length();
                    newNode.address = loc;
                    newNode.writeNode(loc); //writing it at the length
                }else{ //write this node to the freelist
                    //make sure to update loc so that it still reflects the address of newNode in the file

                }
                split = true; //setting split to true

                while(!path.empty() && split){
                   BTreeNode node = path.pop();

                   if(node.count < order-1){//there is room in the node for this value
                        //we need to insert val and loc to this node and update the count
                        updateNode(node, val, loc);
                        split = false;
                    }else{ //there is not room in node for a new value 
                        newNode = new BTreeNode();
                        newNode = split(node, val, loc, false); //we are splitting the root. the right values node is newNode and node is now the left values node
                        val = newNode.keys[0];
                        node.writeNode(node.address);
                        //now writing newNode to the file
                        if(isFreeEmpty()){
                            loc = f.length();
                            newNode.address = loc;
                            newNode.writeNode(loc);
                        }else{

                        }
                        newNode.writeNode(newNode.address);
                        cur = node;
                        //split remains true

                   }
                }
                if(split){//the root was split
                    newNode = new BTreeNode();
                    newNode.keys = keysArray(val, newNode.keys); //adding val to newNode
                    newNode.children[0] = cur.address;
                    newNode.children[1] = loc;
                    newNode.count = getCount(newNode.keys, false); //this is the new root. updating its count and it is not a leaf
                    //writing the node and updating the root
                    if(isFreeEmpty()){ //writing it at the length
                        long address = f.length();
                        newNode.address = address;
                        newNode.writeNode(address);
                        writeRoot(address);
                        this.root = address;

                    }else{ //writing this node at the next available spot in the freelist

                    }
                }
            }
        }
        return true;
    }

    public boolean isInTree(int k) throws IOException{
        //start with search path
        BTreeNode cur = new BTreeNode(root);
        while(!cur.isLeaf){
            int index = compareForChildrenLeaves(k, cur.keys);
            cur = new BTreeNode(cur.children[index]);
        }//now at the row were it would be contained
        for(int i = 0; i<cur.keys.length; i++){
            if(cur.keys[i] == k)return true;
        }
        return false;
    }

    /*
     * This is a method that updates a BTreeNode by adding the parameter key, adding
     * the parameter address, and updating the count of the BTreeNode
     */
    public void updateNode(BTreeNode newNode, int key, long addr) throws IOException{
        int childIndex = compareForChildrenLeaves(key, newNode.keys) + 1; //this is the index of where the address will go
        newNode.children = shiftChildren(childIndex, newNode.children); //shifting the children
        newNode.children[childIndex] = addr; //setting the address
        newNode.keys = keysArray(key,newNode.keys); //updating the key
        newNode.count = getCount(newNode.keys, newNode.isLeaf);
        newNode.writeNode(newNode.address);
    }

    /*
     * This is a simple method that updates the root in the file
     */
    public void writeRoot(long address) throws IOException{
        f.seek(0);
        f.writeLong(address);
    }

    /*
     * This method returns a BTreeNode. It returns the right values node and modifies the parameter node to be the left
     * it also takes the key as a parameter along with the long address to be added to children 
     */
    public BTreeNode split(BTreeNode left, int key, long address, boolean leaf){
        BTreeNode right = new BTreeNode();
        //creating the key arrays for the nodes
        left.keys = keysNotNullArray(key, left.keys); //this adds the key to the array;
        int index = 1;
        if(leaf) index = compareForChildren(key, left.keys);
        if(!leaf) index = compareForChildrenLeaves(key, left.keys);
        //index = compareForChildrenLeaves(key, left.keys);
        left.children = shiftChildren(index,left.children); //shifting the children
        left.children[index] = address; //adding the new address

        right.keys = splitKeysRight(left.keys); //splitting the keys now
        left.keys = splitKeysLeft(left.keys);
        //the keys of both are split

        right.children = splitChildrenRight(left.children);
        if(order%2 == 0){
            if(leaf) right.children[order-1] = right.children[(int)Math.ceil((double)right.children.length/2)-1]; //this is if it is a leaf setting the pointer values to the next node to 0
            if(leaf) right.children[(int)Math.ceil((double)right.children.length/2)-1] = 0; //this is if it is a leaf setting the pointer values to the next node to 0
        }else{
            if(leaf) right.children[order-1] = right.children[(int)Math.ceil((double)right.children.length/2)]; //this is if it is a leaf setting the pointer values to the next node to 0
            if(leaf) right.children[(int)Math.ceil((double)right.children.length/2)] = 0; //this is if it is a leaf setting the pointer values to the next node to 0
        }
        left.children = splitChildrenLeft(left.children);
        //the children of both are split

        //Now updating the nodes counts (Negative because they are both leaves)
        right.count = getCount(right.keys, leaf);
        left.count = getCount(left.keys, leaf);
        

        return right;

    }

    /*
     * This is a method that updates the count of a node
     */

    /*
     * Returns an array of the left keys
     */
    public int[] splitKeysLeft(int[] arr){
        int[] ret = new int[order];
        for(int i = 0; i<ret.length; i++){
            if(i < Math.floor((double)ret.length/2)){
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
    public int[] splitKeysRight(int[] arr){
        int[] ret = new int[order];
        int x = 0;
        for(int i = (int)Math.floor((double)ret.length/2); i<order+(int)Math.floor((double)ret.length/2); i++){
            if(i<order){
                ret[x] = arr[i];
                x++;
            }else{
                ret[x] = Integer.MIN_VALUE;
                x++;
            }
        }
        return ret;
    }

    /*
     * Returns an array of the left children pointers 
     * parameter address is the new address associated with the new key
     */
    public long[] splitChildrenLeft(long[] arr){
        long[] ret = new long[order+1];
        for(int i = 0; i< Math.ceil((double)order/2)+1; i++){ //changed to ceil
            ret[i] = arr[i];
        }
        return ret;
    }

    /*
     * Returns an array of the right children pointers
     * parameter address is the new address associated with the new key
     */
    public long[] splitChildrenRight(long[] arr){
        long[] ret = new long[order+1];
        int x = 0; //counter
        for(double i = Math.ceil((double)order/2); i<ret.length; i++){
            ret[x] = arr[(int)i];
            x++;
        }
        return ret;
    }

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

        /*
         * Specifically for shifting the children of leaves
         */
        public long[] shiftChildrenLeaves(int index, long[] children){
        long temp = 0;
        long t;
        for(int i = index; i<children.length-3; i++){
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
            int index = compareForChildrenLeaves(k, cur.keys);
            cur = new BTreeNode(cur.children[index]);
            ret.push(cur); //pushing the next node in the search path onto the stack
        }
        return ret;
    }

    /*
     * This is a method that checks the key array of a node for a key
     * returns true if the key is in the array (duplicate) else false
     */
    public boolean checkArrForKey(int key, int[] arr){
        for(int i = 0; i<arr.length; i++){
            if(arr[i] == key) return false;
        }
        return true;
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
     * This returns the index with leaves
     */
    public int compareForChildrenLeaves(int key, int[] keyArr){
        if(key < keyArr[0]) return 0;
        if(key >= keyArr[order-1] && keyArr[order-1] != Integer.MIN_VALUE) return order;
        for(int i = 0; i<keyArr.length; i++){
            if(key >= keyArr[order-2] && keyArr[order-2] != Integer.MIN_VALUE) return order-1;
            if(keyArr[i+1] == Integer.MIN_VALUE) return i+1;
            if(key >= keyArr[i] && key < keyArr[i+1]) return i+1;
        }
        return 0;
    }




    public LinkedList<Long> rangeSearch(int low, int high) throws IOException{
        //PRE: low <= high 
        LinkedList<Long> ret = new LinkedList<Long>();
        
        Stack<BTreeNode> stack = searchWithStack(root, low);
        BTreeNode lowLeaf = stack.pop(); //this is the low leaf
        int curKey = 0;
        int i = 0;

        while(curKey < high){ //search until the highest value is found
            curKey = lowLeaf.keys[i];
            if(curKey >= low && curKey <= high && curKey != Integer.MIN_VALUE) ret.add(lowLeaf.children[i]);
            //otherwise leave the leaf node and go to the next one
            if(i == order-2){
                if(lowLeaf.children[i+1] == 0) break; //this is the case where we cant have a bigger value
                lowLeaf = new BTreeNode(lowLeaf.children[i+1]);
                i = 0;
                continue;
            }
            i++;
        }
            



       


//        return a list of row addresses for all keys in the range low to high inclusive 
//        the implementation must use the fact that the leaves are linked 
//        return an empty list when no keys are in the range 
        return ret; 
    }


    public void printNode(long address) throws IOException{
        BTreeNode cur = new BTreeNode(address);
        System.out.println("\n-------------------");
        System.out.println("This is the content: "+cur.count);
        System.out.println("This is the address: "+cur.address);
        System.out.println("This is the isLeaf value: "+cur.isLeaf);
        System.out.print("These are the keys: ");
        for(int i = 0; i<order-1; i++){
            System.out.print(cur.keys[i]+" ");
        }
        System.out.println();
        System.out.print("These are the children: ");
        for(int i = 0; i<order; i++){
            System.out.print(cur.children[i]+" ");
        }
        System.out.println("\n-------------------");
    }

    public void print() throws IOException{
        System.out.println("-- Printing the Tree --");
        Queue<Long> queue = new ArrayDeque<Long>();
        queue.add(root);
        while(!queue.isEmpty()){
            long cur = queue.poll();
            BTreeNode now = new BTreeNode(cur);
            if(now.isLeaf){}
            else{
                for(int i = 0; i < now.children.length; i++){
                    if(now.children[i] == 0){}
                    else{queue.add(now.children[i]);}
                }
            }
            printNode(cur);
            
        }

    }


    public RandomAccessFile getFile(){
        return f;
    }

    public void close() throws IOException { 
        //close the B+tree. The tree should not be accessed after close is called 
        //writing out everything.
        f.seek(0);
        f.writeLong(root);
        f.writeLong(free);
        f.writeInt(blockSize);
        f.close();

    }
    
    public static void main(String[] args) throws IOException{
        BTree tree = new BTree("./BtreeFile", 132);
        tree.insert(0, 12);
        tree.insert(25, 13);
        tree.insert(1, 14);
        tree.insert(26, 15);
        tree.insert(2, 16);
        tree.insert(27, 17);
        tree.insert(3, 18);
        tree.insert(28, 19);
        tree.insert(4, 20);
        tree.insert(29, 21);
        tree.insert(5, 22);
        tree.insert(30, 23);
        tree.insert(6, 24);
        tree.insert(31, 25);
        tree.insert(7, 26);
        tree.insert(32, 27);
        tree.insert(8, 28);
        tree.insert(33, 29);
        tree.insert(9, 30);
        tree.insert(10, 31);
        tree.insert(35, 31);
        tree.insert(11, 31);
        tree.insert(36, 31);
        tree.insert(12, 31);
        //tree.insert(37, 31);
            
            
            
            
            
         
        tree.print();
    }
}


