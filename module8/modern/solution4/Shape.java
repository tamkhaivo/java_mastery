package module8.modern.solution4;

// Sealed interface: We permit strictly defined implementations.
public sealed interface Shape permits Circle, Rectangle {
    // No "accept" method needed => Data and Logic are decoupled!
}
