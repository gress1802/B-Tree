public class Test {
    public static void main(String[] args){
        int[] keyArr = {Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MIN_VALUE};
        //int[] keyArr = {5,10,20,0};
        int[] z = keysArray(40,keyArr);
        z = keysArray(50,keyArr);
        z = keysArray(55,keyArr);
        z = keysArray(35,keyArr);
        for(int i = 0; i<z.length; i++){
            System.out.println(z[i]);
        }
        System.out.println(keysArray(0, keyArr));
    }

    public static int[] keysArray(int add, int[] curArray){// add is the int we will add to the array and curArray is the array we will add it to
        if(curArray[0] == Integer.MIN_VALUE){
            curArray[0] = add;
            return curArray;
        }
        int n = 0;
        for(int i = 0; i<curArray.length; i++){
            if(curArray[i] != Integer.MIN_VALUE) n++;
        }
        if(n>=4) return null; //returning null because the array is full
        int i;
        for(i = n - 1; (i>= 0 && curArray[i] > add); i--){
            curArray[i+1] = curArray[i];
        }
        curArray[i+1] = add;
        return curArray;
    }
}
