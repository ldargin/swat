/**
<p>Provides the implementation of {@link Swat}.
{@link Swat} is the root of a set of editors
that are integrated into a single java application.</p>

<p>Each editor is designed to handle a different part of the world model.
The current editors are:
<ul>
<li>{@link com.storytron.swat.verbeditor.VerbEditor} For editing verbs. </li>
<li>{@link com.storytron.swat.ActorEditor} For editing actors. </li>
<li>{@link com.storytron.swat.RelationshipEditor} For editing relations between actors. </li>
<li>{@link com.storytron.swat.PropEditor} For editing props. </li>
<li>{@link com.storytron.swat.StageEditor} For editing stages. </li>
</ul>
 </p>
 
 <p>
 Each of this editors has its own class that defines and contains all the 
 controls that appear in the editor. 
  The editors are shown one at a time and are chosen from a menu.</p>

 
 <h2>Lizards</h2>
 {@link Swat} also provides some auxiliary utilities for the user that are called lizards. 
 Current lizards:
 <ul>
 <li>{@link Notes lizard} for searching scripts containing certain node comments.</li>
 <li>{@link com.storytron.swat.loglizard.LogLizard} for analyzing execution  of a story.</li>
 <li>{@link storyTellerPackage.Storyteller} for playing a story.</li>
 <li>{@link Rehearsal} for making rehearsals on the world.</li>
 <li>{@link SearchLizard} for searching scripts.</li>
 <li>{@link Swat.ComesFromLizard} for navigating backwards the verb web.</li>
 </ul>
 */
package com.storytron.swat;