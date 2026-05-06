import java.util.Random;

public class Main {
    public static void main(String[] args) {

        String studentID = "170425501"; 
        int firstTwo = Integer.parseInt(studentID.substring(0, 2));
        int lastDigit = Integer.parseInt(studentID.substring(studentID.length() - 1));
        int songCount = firstTwo + lastDigit;

        LinkedList mainList = new LinkedList();
        LinkedList popList = new LinkedList();
        LinkedList rockList = new LinkedList();
        LinkedList classicList = new LinkedList();
        LinkedList sortedList = new LinkedList();
        CircularLinkedList topLikedList = new CircularLinkedList();

        String[] genres = {"Pop", "Rock", "Klasik"};

        // GERÇEK SARKI İSİMLERİ
        String[] songNames = {
            "Shape of You", "Believer", "Numb", "Imagine",
            "Billie Jean", "Bohemian Rhapsody", "Let It Be",
            "Smells Like Teen Spirit", "Hotel California",
            "Halo", "Rolling in the Deep", "Yesterday",
            "Thunder", "Counting Stars", "Faded",
            "Perfect", "Someone Like You", "Lose Yourself",
            "Hey Jude", "Uptown Funk"
        };

        Random rand = new Random();

        for (int i = 0; i < songCount; i++) {
            String name = songNames[i];
            String genre = genres[rand.nextInt(3)];
            int duration = rand.nextInt(300) + 60; // 60-360 saniye
            int likes = rand.nextInt(1000);

            mainList.add(name, genre, duration, likes);

            if (genre.equals("Pop"))
                popList.add(name, genre, duration, likes);
            else if (genre.equals("Rock"))
                rockList.add(name, genre, duration, likes);
            else
                classicList.add(name, genre, duration, likes);
        }

        System.out.println("ORIGINAL LIST:");
        mainList.printList();

        System.out.println("POP LIST:");
        popList.printList();

        System.out.println("ROCK LIST:");
        rockList.printList();

        System.out.println("KLASIK LIST:");
        classicList.printList();

        // Süreye göre sıralı liste
        Song temp = mainList.head;
        while (temp != null) {
            Song newSong = new Song(temp.name, temp.genre, temp.duration, temp.likes);
            sortedList.sortedInsert(newSong);
            temp = temp.next;
        }

        System.out.println("SORTED BY DURATION:");
        sortedList.printList();

        // En çok beğenilen 6 şarkı (dairesel liste)
        for (int i = 0; i < 6; i++) {
            Song max = mainList.head;
            Song t = mainList.head;

            while (t != null) {
                if (t.likes > max.likes)
                    max = t;
                t = t.next;
            }

            Song newSong = new Song(max.name, max.genre, max.duration, max.likes);
            topLikedList.add(newSong);
            max.likes = -1;
        }

        System.out.println("TOP 6 LIKED SONGS (CIRCULAR LIST):");
        topLikedList.printCircular();
    }
}