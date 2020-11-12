public class Exceptions {
    static class NoInverseElementException extends Exception {
        public NoInverseElementException() {
            super("No inverse element exception");
        }
    }

    static class BadPointException extends Exception {
        public BadPointException() {
            super("Bad point");
        }
    }

    static class NotPrimeException extends Exception {
        public NotPrimeException() {
            super("Not prime number");
        }
    }
}
