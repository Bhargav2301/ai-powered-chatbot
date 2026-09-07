package com.polymath.data

import com.polymath.model.ContentKind

/** Original, source-linked starter lessons. These are evergreen examples, never live news. */
object StarterContent {
    fun items(): List<ContentEntity> = listOf(
        pill("retrieval", "Remember by retrieving", "Reading makes an idea familiar. Recall makes you find it again.",
            "After reading, close the source and explain the central idea in your own words. Then compare your explanation with the source. That comparison exposes gaps that familiarity can hide.\n\nTry it with one concept today: read, close, recall, check. A quiz is evidence of one successful retrieval, not proof of mastery.",
            "science", "The Learning Scientists", "https://www.learningscientists.org/blog/2016/6/23-1",
            "Which action practices retrieval?", listOf("Reread the same paragraph", "Explain the idea with the source closed", "Highlight every sentence"), 1,
            "Retrieval requires bringing information back without looking at the source."),
        pill("feedback", "Build a loop, then improve it", "A system can use its output to change its next action.",
            "A thermostat compares measured temperature with a target and adjusts heating. That is a feedback loop. A project can use the same structure: choose an outcome, take an action, observe the result, and adjust.\n\nWrite down what you will measure before you start. A loop without an observation cannot tell you whether your intervention worked.",
            "systems", "OpenStax", "https://openstax.org/books/biology-2e/pages/1-1-the-science-of-biology",
            "What closes a feedback loop?", listOf("Adding more tasks", "Observing the result and adjusting the next action", "Keeping the original plan unchanged"), 1,
            "The result must influence the next action for the loop to provide feedback."),
        pill("overfit", "A model can memorize the wrong lesson", "Training accuracy alone does not tell you whether a model generalizes.",
            "Overfitting happens when a model fits the training examples too closely and performs poorly on new examples. Evaluate on data that was not used to fit the model. Keep the final test set separate from repeated tuning.\n\nFor your own ideas, ask: does this rule work outside the examples that inspired it?",
            "ai", "Google Machine Learning", "https://developers.google.com/machine-learning/crash-course/overfitting/overfitting",
            "Which observation suggests overfitting?", listOf("Excellent training results, poor results on unseen examples", "Equal results on training and test data", "A smaller file size"), 0,
            "A large gap between training and unseen-data performance can indicate overfitting."),
        pill("affordance", "Make the next action visible", "A gesture is useful only when people can discover it.",
            "A swipe can be fast for someone who already knows it. A visible Save button communicates the same action to a first-time reader and supports assistive technology. Give important actions a clear label and immediate feedback.\n\nAudit one interface: can a new user find the next action without being taught a gesture?",
            "design", "Android Developers", "https://developer.android.com/develop/ui/compose/accessibility/api-defaults",
            "What improves access to a swipe action?", listOf("Hide every button", "Offer an equivalent labeled button", "Require a longer swipe"), 1,
            "An equivalent labeled control makes the action discoverable and accessible."),
        pill("compound", "Compounding depends on the base", "Repeated percentage growth applies to the new total each time.",
            "If 100 grows by 10%, it becomes 110. Another 10% increase applies to 110, producing 121. The second increase is 11, not 10.\n\nWhen using a compounding metaphor for learning, identify what is actually accumulating. Saved articles are a collection; the ability to reuse an idea is a different outcome.",
            "math", "OpenStax", "https://openstax.org/books/precalculus-2e/pages/4-1-exponential-functions",
            "What is 100 after two successive increases of 10%?", listOf("120", "121", "110"), 1,
            "100 × 1.1 × 1.1 = 121."),
        pill("small-test", "Make an assumption testable", "A small experiment needs a result that could change your mind.",
            "Turn a vague belief into a question with an observable outcome. For example: can three people complete the main task in a prototype without help? Decide what success and failure look like before observing the result.\n\nA small test reduces uncertainty. It does not establish that every user will behave the same way.",
            "craft", "GOV.UK Service Manual", "https://www.gov.uk/service-manual/user-research/using-moderated-usability-testing",
            "Which experiment has an observable outcome?", listOf("See whether the idea feels good", "Measure whether participants finish a specified task", "Ask whether the concept is innovative"), 1,
            "Completing a specified task is an observable result you can use to revise the design."),
        pill("cache", "Freshness and storage are different", "A cached response can exist without being fresh enough to reuse.",
            "HTTP caching separates storing a response from deciding whether it can be reused. A cache can revalidate a stored response with the server instead of downloading the entire representation again.\n\nFor a news reader, show the publication date and the last fetch time separately. A recent refresh does not make an old article new.",
            "systems", "MDN", "https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Caching",
            "Does fetching an old article today make its publication date today?", listOf("Yes", "No, publication and fetch time describe different events"), 1,
            "Fetch time describes retrieval; publication time describes the source's publication event."),
        pill("evidence", "Keep the source attached", "A useful answer should let you inspect the evidence behind it.",
            "Retrieval-augmented generation supplies selected documents to a language model before it answers. The retrieval step can bring relevant information into context, but it does not guarantee that the generated statement is supported.\n\nInspect the source passage. A citation that exists can still fail to support the claim beside it.",
            "ai", "RAG paper", "https://arxiv.org/abs/2005.11401",
            "What does a valid source link guarantee?", listOf("Every generated claim is correct", "The linked source can be inspected", "The model cannot make mistakes"), 1,
            "Source visibility enables checking. Existence of a citation alone does not establish support."),
    )
    private fun pill(id: String, title: String, summary: String, body: String, topic: String,
        publisher: String, url: String, question: String, answers: List<String>, correct: Int, explanation: String) =
        ContentEntity("starter:$id", ContentKind.KNOWLEDGE_PILL, title, summary, body, topic,
            publisher, url, null, 0, question, answers, correct, explanation)
}
