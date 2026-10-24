#!/usr/bin/env python3
"""Seed a curated set of story, novel, and programming titles through the SLMS API.

Usage: python3 helper/seed_mock_books.py [--base-url http://localhost:8080]
The admin password is requested securely unless SLMS_ADMIN_PASSWORD is set.
Existing ISBNs are detected through the admin shelf report, so reruns are safe.
"""

import argparse
import getpass
import json
import os
import sys
import urllib.error
import urllib.request


# ISBN-13 values, title, author, subject, shelf, summary, key concepts
BOOKS = [
    ("9780441172719", "Dune", "Frank Herbert", "Science Fiction", "Fiction · A1", "Paul Atreides arrives on the desert planet Arrakis, where control of a rare resource shapes an epic struggle over power, ecology, and destiny.", "Arrakis, ecology, empire, prophecy"),
    ("9780451524935", "Nineteen Eighty-Four", "George Orwell", "Dystopian Fiction", "Fiction · A1", "Winston Smith lives under a regime that monitors private life and rewrites the past, then risks everything for independent thought and human connection.", "surveillance, totalitarianism, truth"),
    ("9780061120084", "To Kill a Mockingbird", "Harper Lee", "Classic Fiction", "Fiction · A1", "Through Scout Finch’s childhood in Alabama, a family and a town confront racial injustice, compassion, and the limits of courage.", "justice, empathy, childhood"),
    ("9780547928227", "The Hobbit", "J. R. R. Tolkien", "Fantasy", "Fiction · A1", "Bilbo Baggins leaves home with a company of dwarves on a journey to reclaim their mountain home and its treasure from a dragon.", "adventure, courage, Middle-earth"),
    ("9780141439518", "Pride and Prejudice", "Jane Austen", "Classic Fiction", "Fiction · A1", "Elizabeth Bennet navigates family expectations, first impressions, and questions of class and character in Regency England.", "courtship, class, family"),
    ("9781503280786", "Moby-Dick", "Herman Melville", "Classic Fiction", "Fiction · A1", "Ishmael joins Captain Ahab’s voyage aboard the Pequod, a whaling ship whose commander is consumed by pursuit of a white whale.", "obsession, fate, the sea"),
    ("9780486282114", "Frankenstein", "Mary Shelley", "Gothic Fiction", "Fiction · A1", "Victor Frankenstein creates a living being and abandons it, setting in motion a tragedy about responsibility, isolation, and ambition.", "creation, responsibility, isolation"),
    ("9780141441146", "Jane Eyre", "Charlotte Brontë", "Classic Fiction", "Fiction · A1", "An orphaned governess builds an independent life and finds love while holding fast to her conscience and self-respect.", "independence, love, morality"),
    ("9780743273565", "The Great Gatsby", "F. Scott Fitzgerald", "Classic Fiction", "Fiction · A1", "Nick Carraway observes the wealth, longing, and illusion surrounding Jay Gatsby on Long Island in the Jazz Age.", "ambition, class, the American Dream"),
    ("9781451673319", "Fahrenheit 451", "Ray Bradbury", "Science Fiction", "Fiction · A1", "Firefighter Guy Montag begins to question a society that burns books and discourages reflection, conversation, and dissent.", "censorship, knowledge, conformity"),
    ("9780316769488", "The Catcher in the Rye", "J. D. Salinger", "Coming-of-Age Fiction", "Fiction · A1", "Holden Caulfield recounts several unsettled days in New York as he struggles with grief, adulthood, and belonging.", "adolescence, grief, alienation"),
    ("9780451526342", "Animal Farm", "George Orwell", "Political Satire", "Fiction · A1", "Farm animals overthrow their human owner, but a new leadership gradually turns their revolution into another oppressive system.", "revolution, propaganda, power"),
    ("9780060850524", "Brave New World", "Aldous Huxley", "Dystopian Fiction", "Fiction · A1", "A highly engineered society offers comfort and stability at the cost of freedom, family, and individual identity.", "technology, conformity, freedom"),
    ("9780375842207", "The Book Thief", "Markus Zusak", "Historical Fiction", "Fiction · A1", "A young girl in Nazi Germany finds friendship and refuge in books while her foster family hides a Jewish man.", "war, friendship, words"),
    ("9780062315007", "The Alchemist", "Paulo Coelho", "Literary Fiction", "Fiction · A1", "A young Andalusian shepherd travels toward Egypt in search of treasure and discovers how the journey changes his understanding of purpose.", "journey, purpose, dreams"),
    ("9780147514011", "Little Women", "Louisa May Alcott", "Classic Fiction", "Fiction · A1", "The four March sisters grow into adulthood during the American Civil War, each pursuing a distinct idea of work, love, and independence.", "sisterhood, family, ambition"),
    ("9780143058144", "Crime and Punishment", "Fyodor Dostoevsky", "Classic Fiction", "Fiction · A1", "In St. Petersburg, student Rodion Raskolnikov commits a crime and is drawn into a psychological struggle over guilt and moral responsibility.", "guilt, morality, redemption"),
    ("9780140268867", "The Odyssey", "Homer", "Epic Poetry", "Fiction · A1", "Odysseus faces monsters, storms, and difficult choices on his long journey home after the Trojan War.", "homecoming, loyalty, endurance"),
    ("9780590353427", "Harry Potter and the Sorcerer’s Stone", "J. K. Rowling", "Fantasy", "Fiction · A1", "On his eleventh birthday, Harry Potter discovers he is a wizard and begins a new life at Hogwarts School of Witchcraft and Wizardry.", "friendship, magic, courage"),
    ("9780439023481", "The Hunger Games", "Suzanne Collins", "Science Fiction", "Fiction · A1", "Katniss Everdeen volunteers for a televised fight to the death and challenges the rules of a powerful regime.", "survival, inequality, resistance"),
    ("9780618640157", "The Lord of the Rings: The Fellowship of the Ring", "J. R. R. Tolkien", "Fantasy", "Fiction · A1", "Frodo Baggins sets out from the Shire with a fellowship to destroy a ring that could place Middle-earth under Sauron’s control.", "fellowship, sacrifice, good and evil"),
    ("9780756404741", "The Name of the Wind", "Patrick Rothfuss", "Fantasy", "Fiction · A1", "The gifted Kvothe recounts his difficult childhood, education, and early adventures in a world shaped by music and magic.", "storytelling, magic, identity"),
    ("9780553593716", "A Game of Thrones", "George R. R. Martin", "Fantasy", "Fiction · A1", "Rival noble families struggle for power across Westeros as threats gather beyond the realm’s northern boundary.", "politics, loyalty, power"),
    ("9780399590504", "Educated", "Tara Westover", "Memoir", "Fiction · A1", "Tara Westover describes growing up in an isolated family and pursuing education beyond the world she had known.", "education, family, self-invention"),
    ("9780735219090", "Where the Crawdads Sing", "Delia Owens", "Mystery", "Fiction · A1", "A young woman raised in the marshes of North Carolina becomes the focus of a murder investigation in a nearby town.", "nature, isolation, mystery"),
    ("9781250301697", "The Silent Patient", "Alex Michaelides", "Mystery", "Fiction · A1", "A psychotherapist becomes fascinated by a painter who stopped speaking after her husband was murdered.", "therapy, secrets, unreliable narration"),
    ("9780525559474", "The Midnight Library", "Matt Haig", "Speculative Fiction", "Fiction · A1", "Between life and death, Nora Seed explores alternate lives shaped by choices she once made or left behind.", "regret, possibility, purpose"),
    ("9780316556347", "Circe", "Madeline Miller", "Mythological Fiction", "Fiction · A1", "The enchantress Circe finds her own power and place among gods, monsters, and mortals from Greek mythology.", "mythology, exile, transformation"),
    ("9780553418026", "The Martian", "Andy Weir", "Science Fiction", "Fiction · A1", "Stranded alone on Mars, astronaut Mark Watney uses engineering, scientific reasoning, and humor to stay alive.", "problem solving, resilience, space"),
    ("9780593135204", "Project Hail Mary", "Andy Weir", "Science Fiction", "Fiction · A1", "An astronaut wakes alone on a distant mission and must use science and ingenuity to solve a threat to life on Earth.", "spaceflight, science, cooperation"),
    ("9780345391803", "The Hitchhiker’s Guide to the Galaxy", "Douglas Adams", "Science Fiction", "Fiction · A1", "Arthur Dent is swept off Earth and into a comic journey across the galaxy with a guidebook and an unlikely group of companions.", "absurdity, space, humor"),
    ("9780441569595", "Neuromancer", "William Gibson", "Science Fiction", "Fiction · A1", "A washed-up computer hacker is hired for a dangerous job in a future shaped by artificial intelligence and global networks.", "cyberspace, AI, technology"),
    ("9780553293357", "Foundation", "Isaac Asimov", "Science Fiction", "Fiction · A1", "Hari Seldon uses mathematical predictions to plan for the preservation of knowledge through the fall of a galactic empire.", "empire, history, prediction"),
    ("9780812550702", "Ender’s Game", "Orson Scott Card", "Science Fiction", "Fiction · A1", "A gifted child is sent to a military school in space to prepare for a conflict with an alien civilization.", "leadership, strategy, childhood"),
    ("9780385490818", "The Handmaid’s Tale", "Margaret Atwood", "Dystopian Fiction", "Fiction · A1", "Offred recounts life in Gilead, a regime that strips women of rights and uses them to control reproduction.", "rights, power, resistance"),
    ("9781400033416", "Beloved", "Toni Morrison", "Historical Fiction", "Fiction · A1", "Sethe, a formerly enslaved woman, is haunted by the past as she tries to build a life with her daughter after the Civil War.", "memory, family, freedom"),
    ("9781594631931", "The Kite Runner", "Khaled Hosseini", "Literary Fiction", "Fiction · A1", "Amir looks back on childhood in Kabul and later returns to Afghanistan seeking a way to make amends.", "friendship, guilt, redemption"),
    ("9780062316097", "Sapiens: A Brief History of Humankind", "Yuval Noah Harari", "History", "Nonfiction · B1", "A broad account of human history considers how shared stories, agriculture, science, and institutions shaped societies.", "human history, culture, society"),
    ("9780374533557", "Thinking, Fast and Slow", "Daniel Kahneman", "Psychology", "Nonfiction · B1", "Daniel Kahneman describes two modes of thought and examines how they shape judgment, choice, and common reasoning errors.", "cognition, decision making, bias"),
    ("9780735211292", "Atomic Habits", "James Clear", "Self-Development", "Nonfiction · B1", "A practical guide to building useful routines and changing behavior through small, repeatable improvements.", "habits, behavior, routines"),
    ("9780262035613", "Deep Learning", "Ian Goodfellow, Yoshua Bengio, and Aaron Courville", "Artificial Intelligence", "Computing · C1", "A textbook covering the mathematical foundations and core methods behind modern deep learning systems.", "neural networks, machine learning, optimization"),
    ("9781449373320", "Designing Data-Intensive Applications", "Martin Kleppmann", "Distributed Systems", "Computing · C1", "An engineering guide to reliable, scalable, and maintainable systems for processing and storing data.", "distributed systems, databases, reliability"),
    ("9780134610993", "Artificial Intelligence: A Modern Approach", "Stuart Russell and Peter Norvig", "Artificial Intelligence", "Computing · C1", "A comprehensive introduction to artificial intelligence, from search and logic to learning, robotics, and decision making.", "AI, search, machine learning"),
    ("9780134092669", "Computer Systems: A Programmer’s Perspective", "Randal E. Bryant and David R. O’Hallaron", "Computer Systems", "Computing · C1", "A programmer-focused introduction to how processors, memory, linking, exceptional control flow, and networks work.", "computer architecture, memory, systems"),
    ("9780262046305", "Introduction to Algorithms", "Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest, and Clifford Stein", "Algorithms", "Computing · C1", "A detailed reference to algorithm design and analysis, covering core data structures and a wide range of algorithms.", "algorithms, data structures, complexity"),
    ("9780134685991", "Effective Java", "Joshua Bloch", "Java Programming", "Computing · C1", "Practical guidance for writing safer, clearer, and more maintainable Java programs using established language practices.", "Java, API design, software quality"),
    ("9781491910771", "Head First Java", "Kathy Sierra and Bert Bates", "Java Programming", "Computing · C1", "An accessible introduction to Java programming, object-oriented concepts, and the tools used to build applications.", "Java, object-oriented programming, classes"),
    ("9780134757599", "Refactoring: Improving the Design of Existing Code", "Martin Fowler", "Software Engineering", "Computing · C1", "A catalogue of code transformations for improving software structure while preserving observable behavior.", "refactoring, code quality, testing"),
    ("9780134494166", "Clean Architecture", "Robert C. Martin", "Software Engineering", "Computing · C1", "Principles and patterns for organizing software systems so their policies remain independent of frameworks and details.", "architecture, boundaries, maintainability"),
    ("9780262510875", "Structure and Interpretation of Computer Programs", "Harold Abelson and Gerald Jay Sussman", "Computer Science", "Computing · C1", "A foundational text that uses programming to explore abstraction, computation, and the design of larger systems.", "abstraction, Lisp, computation"),
    ("9780131103627", "The C Programming Language", "Brian W. Kernighan and Dennis M. Ritchie", "C Programming", "Computing · C1", "The classic authors’ guide to the C language, covering its syntax, standard library, and practical programming techniques.", "C, systems programming, language design"),
    ("9781593279509", "Eloquent JavaScript", "Marijn Haverbeke", "JavaScript Programming", "Computing · C1", "A hands-on introduction to JavaScript, programming fundamentals, browser environments, and asynchronous applications.", "JavaScript, web development, programming"),
    ("9781718502703", "Python Crash Course", "Eric Matthes", "Python Programming", "Computing · C1", "A project-based introduction to Python that builds programming fundamentals through practical applications.", "Python, programming, projects"),
    ("9781491946008", "Fluent Python", "Luciano Ramalho", "Python Programming", "Computing · C1", "A detailed guide to using Python’s data model, built-in types, functions, and concurrency features effectively.", "Python, data model, idioms"),
    ("9781617292231", "Grokking Algorithms", "Aditya Y. Bhargava", "Algorithms", "Computing · C1", "An illustrated introduction to common algorithms and data structures, with approachable examples and visual explanations.", "algorithms, searching, sorting"),
    ("9781985086593", "Operating Systems: Three Easy Pieces", "Remzi H. Arpaci-Dusseau and Andrea C. Arpaci-Dusseau", "Operating Systems", "Computing · C1", "A free-textbook-style introduction to virtualization, concurrency, and persistence in operating systems.", "operating systems, processes, concurrency"),
    ("9780078022159", "Database System Concepts", "Abraham Silberschatz, Henry F. Korth, and S. Sudarshan", "Databases", "Computing · C1", "A comprehensive introduction to database systems, including relational models, SQL, transactions, and storage.", "databases, SQL, transactions"),
    ("9780321486813", "Compilers: Principles, Techniques, and Tools", "Alfred V. Aho, Monica S. Lam, Ravi Sethi, and Jeffrey D. Ullman", "Compilers", "Computing · C1", "A foundational treatment of compiler construction, from lexical analysis and parsing to optimization and code generation.", "compilers, parsing, optimization"),
    ("9780735619678", "Code Complete", "Steve McConnell", "Software Engineering", "Computing · C1", "A practical reference on the craft of software construction, including design, coding, debugging, and testing.", "software construction, design, debugging"),
    ("9780321965516", "Don’t Make Me Think", "Steve Krug", "User Experience", "Computing · C1", "A concise guide to web usability that focuses on clear navigation, understandable pages, and user-centered design.", "usability, web design, accessibility"),
    ("9780201835953", "The Mythical Man-Month", "Frederick P. Brooks Jr.", "Software Engineering", "Computing · C1", "Essays on software project management explore estimation, communication, team structure, and the costs of complexity.", "project management, teams, software"),
    ("9780321125217", "Domain-Driven Design", "Eric Evans", "Software Architecture", "Computing · C1", "A software design approach that aligns complex implementations with a carefully developed model of the business domain.", "domain modeling, architecture, software design"),
    ("9780321127426", "Patterns of Enterprise Application Architecture", "Martin Fowler", "Software Architecture", "Computing · C1", "A catalogue of patterns for structuring enterprise applications and their interactions with databases and user interfaces.", "enterprise software, architecture, design patterns"),
]


def request_json(base_url, path, method="GET", token=None, payload=None):
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    headers = {"Accept": "application/json"}
    if data is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(base_url + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            body = response.read()
            return json.loads(body) if body else None
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", "replace")
        try:
            detail = json.loads(detail).get("message", detail)
        except (json.JSONDecodeError, AttributeError):
            pass
        raise RuntimeError(f"{method} {path} failed ({exc.code}): {detail}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Cannot reach SLMS at {base_url}: {exc.reason}") from exc


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default=os.environ.get("SLMS_BASE_URL", "http://localhost:8080"))
    args = parser.parse_args()
    email = os.environ.get("SLMS_ADMIN_EMAIL", "admin@library.local")
    password = os.environ.get("SLMS_ADMIN_PASSWORD") or getpass.getpass(f"Admin password for {email}: ")

    login = request_json(args.base_url.rstrip("/"), "/api/auth/admin/login", "POST", payload={"email": email, "password": password})
    token = login.get("token") if login else None
    if not token:
        raise RuntimeError("Admin login response did not include a bearer token")

    existing = set()
    for shelf in request_json(args.base_url.rstrip("/"), "/api/admin/shelves", token=token) or []:
        for book in shelf.get("books", []):
            existing.add(book.get("isbn"))

    created = 0
    skipped = 0
    failures = []
    for index, (isbn, title, author, category, shelf, summary, concepts) in enumerate(BOOKS, start=1):
        if isbn in existing:
            skipped += 1
            print(f"[{index:02}/{len(BOOKS)}] already exists: {title}")
            continue
        barcode = f"MOCK-{isbn[-7:]}"
        payload = {
            "isbn": isbn,
            "title": title,
            "author": author,
            "coverColor": category,
            "summary": summary,
            "characters": concepts,
            "shelfLocation": shelf,
            "initialBarcodes": [barcode],
        }
        try:
            request_json(args.base_url.rstrip("/"), "/api/admin/books", "POST", token, payload)
            created += 1
            print(f"[{index:02}/{len(BOOKS)}] added: {title}")
        except RuntimeError as exc:
            failures.append((title, str(exc)))
            print(f"[{index:02}/{len(BOOKS)}] FAILED: {title}: {exc}", file=sys.stderr)

    print(f"\nDone: {created} added, {skipped} already present, {len(failures)} failed (catalogue set: {len(BOOKS)} books).")
    if failures:
        return 1
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (RuntimeError, KeyboardInterrupt) as exc:
        print(f"Seed stopped: {exc}", file=sys.stderr)
        raise SystemExit(1)
