#!/usr/bin/env python3

"""
Console Calculator (Python)
Supports: +, -, *, / (protected from division by zero)
Input/output through console; exits on 'exit' or 'quit'.
PEP8-compliant and user-friendly.
"""

def calculate(a, b, operator):
    if operator == '+':
        return a + b
    elif operator == '-':
        return a - b
    elif operator == '*':
        return a * b
    elif operator == '/':
        if b == 0:
            raise ZeroDivisionError('Division by zero is not allowed.')
        return a / b
    else:
        raise ValueError(f"Unsupported operator: {operator}")

def main():
    print("Simple Console Calculator. Type 'exit' or 'quit' to leave.")
    while True:
        user_input = input("Enter expression (format: a operator b): ").strip()
        if user_input.lower() in {'exit', 'quit'}:
            print("Goodbye!")
            break
        try:
            parts = user_input.split()
            if len(parts) != 3:
                print("Invalid format. Use: a operator b (e.g. 5 + 3)")
                continue
            a_str, operator, b_str = parts
            a = float(a_str)
            b = float(b_str)
            result = calculate(a, b, operator)
            print(f"Result: {result}")
        except ValueError as ve:
            print(f"Input error: {ve}")
        except ZeroDivisionError as zde:
            print(f"Calculation error: {zde}")
        except Exception as ex:
            print(f"Unexpected error: {ex}")

if __name__ == "__main__":
    main()
