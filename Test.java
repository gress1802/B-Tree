public class Test {
    public static void main(String[] args){
        int[] arr = {7, 11, 15, 18};
        System.out.println(compareForChildren(16, arr));
    }

    public static int compareForChildren(int key, int[] keyArr){
        int order = 5;
        if(key < keyArr[0]) return 0;
        for(int i = 0; i<keyArr.length; i++){
            if(key >= keyArr[order-2] && keyArr[order-2] != Integer.MIN_VALUE) return order-1;
            if(keyArr[i+1] == Integer.MIN_VALUE) return i+1;
            if(key >= keyArr[i] && key < keyArr[i+1]) return i+1;
        }
        return 0;
    }
}
