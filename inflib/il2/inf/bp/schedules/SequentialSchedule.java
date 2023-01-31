package il2.inf.bp.schedules;

import il2.model.Domain;
import il2.model.Table;
import il2.util.Pair;

import java.util.Iterator;

public class SequentialSchedule extends MessagePassingScheduler {
    MessageIterable iterable;
    Pair[] schedule;

    public SequentialSchedule(Table[] tables) {
        super(tables);
        this.schedule = this.pairs;
        this.iterable = new MessageIterable(this.schedule);
    }
    public SequentialSchedule(Table[] tables, Pair[] schedule) {
        super(tables);
        this.schedule = schedule;
        this.iterable = new MessageIterable(this.schedule);
    }
    public Iterable<Pair> nextIteration() {
        iterable.reset();
        return iterable;
    }
    public boolean isAsynchronous() { return true; }

    class MessageIterable implements Iterable<Pair>, Iterator<Pair> {
        Pair[] pairs;
        int index;
        public MessageIterable(Pair[] p) { pairs = p; index = 0; }
        public Iterator<Pair> iterator() { return this; }
        public boolean hasNext() { return index < pairs.length; }
        public Pair next() { return pairs[index++]; }
        public void remove() {}
        public void reset() { index = 0; }
    }
}
