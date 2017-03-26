# double-booked

When maintaining a calendar of events, it is important to know if an event overlaps with another event.

Given a sequence of events, each having a start and end time, the provided functions in this library will return the sequence of all pairs of overlapping events.

For the purposes of this coding challenge, I have included two implementations to illustrate my thought process and discuss the tradeoffs between the two. Both are correct and return the same output, yet they differ in efficiency. I will document them both below.

(**Assumption**: start and end times are non-inclusive; an event that starts at time t0 will not conflict with an event that ends at time t0)

### get-conflicts-naive

This is simply the naive algorithm for solving this problem. It uses the Clojure combinatorics library to create all possible pairs of events (resulting in n choose 2 pairs), and filters through them. The filter keeps those pairs who have time overlaps, and discards all those who don't.

This solution is simple, easy to understand, and results in less code. However, it is inefficient, and we can do better.

**Total time complexity**: O(n^2)

### get-conflicts-intervals

This solution starts by splitting up Events into their start and end components. It then puts these components into a list, and sorts that list. This sorted list represents what you would see if you were to stack all events on a calendar. This means that, if one start component occurs before a previous event has been closed off by its end component, those two events overlap.

The next step involves scanning through the list of components. For every start component, new conflicts are potentially accumulated. This is done by a set union of all pairs of newly 'active' events (meaning their start has been seen, but not their end) and the previously recorded conflicts. Union is used to ensure no duplicates are recorded.

Sort time complexity: O(n log n)

Scan time complexity: O(n)

**Total time complexity**: O(n log n)

## Usage

Both of the provided functions accept a list of Event records, which has the following definition:

```clojure
(defrecord Event [name start end])
```

The `start` and `end` fields are ISO 8601 datetime strings. How precise these strings are is up to the user (up to millisecond specificity).

Events are created like this:

```clojure
(def e1 (make-event "Winter Break" "1995-12-22" "1996-01-05"))
(def e2 (make-event "Christmas" "1995-12-25" "1995-12-26"))
```

And the functions are called like this:

```clojure
(get-conflicts-naive [e1 e2])
(get-conflicts-intervals [e1 e2])
```

## Testing

Tests have been provided for these two implementations. They are quite simple, and are only there to demonstrate that the implementations work, and give the same results. Tests can be run using the standard `lein test`
