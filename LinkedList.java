class LinkedList {
    Song head;

    void add(String name, String genre, int duration, int likes) {
        Song newSong = new Song(name, genre, duration, likes);
        if (head == null) {
            head = newSong;
        } else {
            Song temp = head;
            while (temp.next != null) {
                temp = temp.next;
            }
            temp.next = newSong;
        }
    }

    void printList() {
        Song temp = head;
        while (temp != null) {
            System.out.println(temp.name + " | " + temp.genre + " | " + temp.duration + " sn | Likes: " + temp.likes);
            temp = temp.next;
        }
        System.out.println();
    }

    void sortedInsert(Song newSong) {
        if (head == null || newSong.duration < head.duration) {
            newSong.next = head;
            head = newSong;
        } else {
            Song temp = head;
            while (temp.next != null && temp.next.duration < newSong.duration) {
                temp = temp.next;
            }
            newSong.next = temp.next;
            temp.next = newSong;
        }
    }
}
