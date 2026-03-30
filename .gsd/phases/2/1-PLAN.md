---
phase: 2
plan: 1
wave: 1
---

# Plan 2.1: Registry Fix & Rasa Config

## Objective
Enable Windows Long Paths to prevent training errors, and scaffold the exact Rasa pipeline as requested in the decisions.

## Context
- .gsd/DECISIONS.md (003, 004, 005)

## Tasks

<task type="auto">
  <name>Apply Windows Long Path Registry Fix</name>
  <action>
    Run PowerShell command to enable LongPathsEnabled in the registry.
    Requires administrative permissions (or run as current user if possible).
  </action>
  <verify>Get-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" -Name "LongPathsEnabled"</verify>
  <done>LongPathsEnabled is set to 1</done>
</task>

<task type="auto">
  <name>Create config.yml</name>
  <files>config.yml</files>
  <action>
    Create a minimal config.yml file configured strictly for NLU (no core policies yet, or default policies).
    Pipeline must use WhitespaceTokenizer and DIETClassifier. No SpaCy/BERT.
    Add CountVectorsFeaturizer and LexicalSyntacticFeaturizer.
  </action>
  <verify>Get-Content config.yml</verify>
  <done>config.yml exists with WhitespaceTokenizer and DIETClassifier explicitly defined</done>
</task>

## Success Criteria
- [ ] Environment supports long paths.
- [ ] `config.yml` is correctly formatted.
