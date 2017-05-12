package ru.shishmakov.core;

import org.junit.Test;

import java.util.NavigableSet;
import java.util.TreeSet;

import static org.apache.commons.lang3.ArrayUtils.isSorted;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public class WordTest {

    @Test
    public void wordShouldBeSortedByWordsFrequency() {
        final NavigableSet<Word> members = new TreeSet<>();
        for (int i = 1; i <= 10; i++) {
            members.add(new Word(String.valueOf(i), 100L * i));
        }
        members.add(new Word("Wikipedia", 2L));
        members.add(new Word("Sasha", 999_999L));
        members.add(new Word("Li", 999_999L));
        members.add(new Word("Wikipedia", 10L));

        assertTrue("Array should be sorted", isSorted(members.toArray(new Word[members.size()])));
        assertEquals("On head of rating should be max points value", 999_999L, members.first().getQuantity());
        assertEquals("On tail of rating should be min points value", 2, members.last().getQuantity());
    }
}
