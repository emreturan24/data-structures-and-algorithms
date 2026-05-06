class CircularLinkedList {
    Song head = null;

    void add(Song newSong) {
        if (head == null) {
            head = newSong;
            head.next = head;
        } else {
            Song temp = head;
            while (temp.next != head) {
                temp = temp.next;
            }
            temp.next = newSong;
            newSong.next = head;
        }
    }

    void printCircular() {
        if (head == null) return;

        Song temp = head;
        do {
            System.out.println(temp.name + " | Likes: " + temp.likes);
            temp = temp.next;
        } while (temp != head);
    }
}