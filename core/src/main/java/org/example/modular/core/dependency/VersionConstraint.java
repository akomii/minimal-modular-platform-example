package org.example.modular.core.dependency;

import java.util.List;

/**
 * A dependency version constraint: an operator ({@code >=}, {@code >}, {@code <=}, {@code <}, {@code =}; a bare version means {@code >=}) and a dotted-numeric version. {@link #satisfies} compares an
 * installed version component-wise, treating missing components as 0.
 */
public final class VersionConstraint {

  // longest operators first, so ">=" matches before ">"
  private static final List<String> OPERATORS = List.of(">=", "<=", ">", "<", "=");

  private final String operator;
  private final String version;

  public VersionConstraint(String constraint) {
    String trimmed = constraint.trim();
    String matchedOperator = ">=";
    String matchedVersion = trimmed;
    for (String candidate : OPERATORS) {
      if (trimmed.startsWith(candidate)) {
        matchedOperator = candidate;
        matchedVersion = trimmed.substring(candidate.length()).trim();
        break;
      }
    }
    this.operator = matchedOperator;
    this.version = matchedVersion;
  }

  public boolean satisfies(String installedVersion) {
    int cmp = compare(installedVersion, version);
    return switch (operator) {
      case ">=" -> cmp >= 0;
      case ">" -> cmp > 0;
      case "<=" -> cmp <= 0;
      case "<" -> cmp < 0;
      default -> cmp == 0;
    };
  }

  private static int compare(String a, String b) {
    String[] left = a.split("\\.");
    String[] right = b.split("\\.");
    int length = Math.max(left.length, right.length);
    for (int i = 0; i < length; i++) {
      int l = i < left.length ? Integer.parseInt(left[i].trim()) : 0;
      int r = i < right.length ? Integer.parseInt(right[i].trim()) : 0;
      if (l != r) {
        return Integer.compare(l, r);
      }
    }
    return 0;
  }
}
