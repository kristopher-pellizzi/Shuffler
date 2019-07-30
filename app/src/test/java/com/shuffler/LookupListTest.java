package com.shuffler;

import com.shuffler.utility.LookupList;

import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;

public class LookupListTest {

    private void printList(List<Integer> list){
        for(int elem : list){
            System.out.printf("%d\t", elem);
        }
        System.out.println();
    }

    @Test
    public void addTest(){
        LookupList<Integer> list = new LookupList<>();
        Random rand = ThreadLocalRandom.current();

        list.add(1);
        list.add(2);
        list.add(3);
        LookupList<Integer> clone = new LookupList<>();
        clone.addAll(list);
        clone.add(2);
        assertEquals(list, clone);
        clone.add(4);
        assertNotEquals(list, clone);
        assertEquals(2, clone.indexOf(3));
        assertEquals(3, clone.indexOf(4));
        clone.remove(1);
        printList(clone);
        assertEquals(1, clone.indexOf(3));
        assertEquals(2, clone.indexOf(4));
        clone.add(2);
        assertEquals(0, clone.indexOf(1));
        assertEquals(1, clone.indexOf(3));
        assertEquals(2, clone.indexOf(4));
        assertEquals(3, clone.indexOf(2));
        clone.pop();
        assertEquals(0, clone.indexOf(1));
        assertEquals(1, clone.indexOf(3));
        assertEquals(2, clone.indexOf(4));
        assertEquals(-1, clone.indexOf(2));
        clone = null;

        for(int i = 0; i < 10000; i++){
            int num = rand.nextInt(1000);
            boolean isContained = list.contains(num);
            LookupList<Integer> newList = new LookupList<>();
            newList.addAll(list);
            newList.add(num);
            if(isContained) {
                //System.out.println("Value already inside the list");
                assertEquals(list, newList);
            }
            else {
                //System.out.println("Value not contained yet");
                assertNotEquals(list, newList);
            }
            list.add(num);
        }
    }
}