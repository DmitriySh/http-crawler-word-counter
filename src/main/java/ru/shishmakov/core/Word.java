package ru.shishmakov.core;


import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
    private final long quantity;

    public Word(String word, long quantity) {
        this.word = word;
        this.quantity = quantity;
    }

    public String getWord() {
        return word;
    }

    public long getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Word)) return false;
        Word other = (Word) o;
        return new EqualsBuilder()
                .append(quantity, other.quantity)
                .append(word, other.word)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(word)
                .append(quantity)
                .toHashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("word", word)
                .add("quantity", quantity)
                .toString();
    }

    @Override
    public int compareTo(@Nonnull Word other) {
        return COMPARATOR.compare(this, checkNotNull(other, "Word is null"));
    }

    private static Comparator<Word> buildMemberComparator() {
        return Comparator.comparing(Word::getQuantity, reverseOrder())
                .thenComparing(w -> w.getWord().length(), reverseOrder());
    }

}
