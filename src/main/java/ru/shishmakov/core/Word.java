package ru.shishmakov.core;


import javax.annotation.Nonnull;
import java.util.Comparator;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Comparator.reverseOrder;

/**
 * @author Dmitriy Shishmakov on 12.05.17
 */
public class Word implements Comparable<Word> {
    private static final Comparator<Word> COMPARATOR = buildMemberComparator();

    private final String word;
    private final Long quantity;

    public Word(String word, Long quantity) {
        this.word = word;
        this.quantity = quantity;
    }

    private static Comparator<Word> buildMemberComparator() {
        return Comparator.comparing(Word::getQuantity, reverseOrder());
    }

    public String getWord() {
        return word;
    }

    public Long getQuantity() {
        return quantity;
    }

    @Override
    public int compareTo(@Nonnull Word other) {
        return COMPARATOR.compare(this, checkNotNull(other, "Word is null"));
    }

}
