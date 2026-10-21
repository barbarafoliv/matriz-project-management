# Simple vs. Structured Prompts

> Learning exercise from the Matriz project: the same request, starting the Matriz application, asked in two different ways.

## 1. The two prompts

**Simple prompt**
> *"I want to create a project and task management application called Matriz. Help me develop the application."*

**Structured prompt**
```text
Goal:         Understand the problem behind Matriz and clarify its
              requirements before any development starts.

Context:      I want to create a project and task management application
              called Matriz. The only description so far is the one in
              the repository README.

Constraints:  Do not propose technologies or implement code.

Task:         Act as a requirements analyst and:
              1. Analyse the problem description.
              2. Identify any missing information.
              3. Formulate clarification questions.

Validation:   Clearly separate known facts from assumptions, so that
              each assumption can be confirmed or rejected.
```

## 2. What happened

- **Simple prompt:** the AI went straight to technical choices and asked which stack (Spring Boot + React was marked "Recommended"), which database and which generic features to use. Without being told, it assumed a web app, Java (from config files only) and typical features. These assumptions only came to light when it was asked to reflect afterwards.
- **Structured prompt:** the AI produced a requirements analysis. It listed 8 ambiguous terms, separated facts traced to their sources from 12 assumptions with risk levels, mapped missing information across 10 areas, and wrote 40 clarification questions prioritised P1–P3. It proposed no technology and wrote no code.

## 3. Comparison

| | Simple prompt | Structured prompt |
|---|---|---|
| **Starting point** | The solution (stack, database) | The problem (goals, users, ambiguity) |
| **Assumptions** | Hidden | Explicit, with risk levels |
| **Questions** | 3 technical, multiple-choice | 40 about the domain, prioritised |
| **Bias** | High ("Recommended" labels) | Low (open questions) |
| **Risk of building the wrong thing** | High | Low |
| **Speed / user effort** | Fast, low effort | Slower, more effort |

**To be fair:** the simple prompt still led the AI to check the repository and ask before coding. Its weakness was that the prompt **didn't say which stage of the work this was**, so the AI guessed "implementation". The structured prompt has limits too: 40 questions is a lot, the AI's own judgement still shaped the priorities, and writing such a prompt takes some knowledge of the process.

## 4. Key lessons

1. **A vague prompt gets the AI's defaults.** If you don't define the task, the AI will.
2. **Set the role and the stage** (requirements, design, implementation, testing).
3. **Say what *not* to do.** Banning solutions keeps the focus on the problem.
4. **Ask for facts and assumptions separately.** It makes the AI's reasoning visible and checkable.
5. **Set the format and a limit** (e.g. "prioritise, max 10 questions").
6. **Iterate:** simple prompt → reflection → structured prompt.

**Reusable template** (Goal → Context → Constraints → Task → Validation):
```text
Goal:         <what you want to achieve and why>.
Context:      <what the project is, what already exists>.
Constraints:  Do not <...>. Only <...>. Format, length, language.
Task:         Act as a <role> and <what to do, step by step>.
Validation:   <how to check the answer, e.g. separate facts from
              assumptions, list open questions, mark uncertain points>.
```

## 5. Conclusion

The simple prompt led to **a solution before the problem was understood**. The structured prompt led to **disciplined requirements analysis**. How clearly a prompt sets the goal, context, constraints, task and validation decides the direction of the answer, and keeps the user in control of the decisions.
