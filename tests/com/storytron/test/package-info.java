/**
Provides implementation for tests.
<p>
These are the things being tested:
<ul>
<li>Cloning Deikto, {@link DeiktoTest#testCloneDeiktoState()}. </li>
<li>Loading and saving Deikto preserves its state, {@link DeiktoTest#testDeiktoInputStream()}. </li>
<li>Saving and loading the engine state preserves it, {@link DeiktoTest#testSaveEngineState()}.</li>
<li>Storyworld reference counter is 0 after all players have logout, {@link JanusDeiktoListTest#testJanusDeiktoList()}.</li>
<li>Stress test for janus (reports average reaction time when multiple player run BoP2K), {@link JanusStressTest#main(String[])}.</li>
<li>Tests for actor editor, {@link SwatTest#testActorEditor()}.</li>
<li>Tests for copyrights editor, {@link SwatTest#testCopyrightEditing()}.</li>
<li>Tests updates of Deikto counters, {@link SwatTest#testDeiktoLimits()}.</li>
<li>Tests for relationship editor, {@link SwatTest#testRelationshipEditor()}.</li>
<li>Tests for relationship settings editor, {@link SwatTest#testRelationshipSettings()}.</li>
<li>Tests for scriptalyzer, {@link SwatTest#testScriptalyzer()}.</li>
<li>Tests for the state stack of verb editor, {@link SwatTest#testVerbEditorStateStack()}.</li>
<li>Tests for relationship window of storyteller, {@link SwatTest#testStorytellerRelationshipsWindow()}.</li>
<li>Test for trivial momentous slider, {@link SwatTest#testVerbPropertiesEditor()}.</li>
<li>Tests adding, renaming and deleting verbs, {@link SwatTest#testVerbEditing()}.</li>
<li>Tests for statistics, {@link UnitTests#testStatistics()}.</li>
</ul>
 */
package com.storytron.test;