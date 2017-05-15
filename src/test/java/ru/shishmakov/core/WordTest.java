package ru.shishmakov.core;

import com.google.common.collect.MinMaxPriorityQueue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public class WordTest {

    @Test
    public void wordShouldBeSortedByWordsFrequency() {
        MinMaxPriorityQueue<Word> priorityQueue = MinMaxPriorityQueue.create();

        for (int i = 1; i <= 30; i++) {
            Word word = new Word(String.valueOf(i), 100L * i);
            priorityQueue.add(word);
        }
        priorityQueue.add(new Word("Wikipedia", 2L));
        priorityQueue.add(new Word("Sasha", 999_999L));
        priorityQueue.add(new Word("Li", 999_999L));
        priorityQueue.add(new Word("Wikipedia", 10L));

        assertEquals("On head of rating should be max points value", 999_999L, priorityQueue.peekFirst().getQuantity());
        assertEquals("On tail of rating should be min points value", 2, priorityQueue.peekLast().getQuantity());
        System.out.println(priorityQueue);
    }
}
