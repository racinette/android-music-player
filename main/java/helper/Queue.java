package helper;

import java.util.NoSuchElementException;

/**
 * Created by prett on 3/8/2018.
 */

public class Queue {
    private int size;

    private Element first;
    private Element last;

    public Queue(){
        size = 0;
    }

    public void addFirst(int i){
        final Element e = new Element(i);

        e.setNext(first);

        if (isEmpty()){
            last = first;
        } else {
            first.setPrevious(e);
        }

        first = e;

        size++;
    }

    public int removeLast() throws NoSuchElementException{
        if (isEmpty()) throw new NoSuchElementException();

        final int value = last.getValue();

        if (size() < 2){
            last = null;
            first = null;
        } else {
            last = last.getPrevious();
            last.setNext(null);
        }

        size--;

        return value;
    }

    public boolean isEmpty(){
        return size == 0;
    }

    public int size(){
        return size;
    }

    private class Element{
        private int value;
        private Element next;
        private Element previous;

        private Element(int i){
            value = i;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public Element getNext() {
            return next;
        }

        public void setNext(Element next) {
            this.next = next;
        }

        public Element getPrevious() {
            return previous;
        }

        public void setPrevious(Element previous) {
            this.previous = previous;
        }
    }

    public Iterator getIterator(){
        return new Iterator();
    }

    public class Iterator{
        private static final int HEAD_ELEMENT_VALUE = -1;

        private Element current;

        private Iterator(){
            current = new Element(HEAD_ELEMENT_VALUE);
            current.setNext(first);
        }

        public boolean hasNext(){
            return current.getNext() != null;
        }

        public int next(){
            current = current.getNext();
            return current.getValue();
        }
    }
}
