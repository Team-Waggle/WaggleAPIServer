#!/usr/bin/env perl
# CLAUDE.md 6절 주석 컨벤션 중 기계 판정이 가능한 규칙만 검사함
# 판단이 필요한 규칙("코드가 말할 수 없는 것만")은 여기서 잡지 못함
use strict;
use warnings;
use utf8;
binmode(STDERR, ':encoding(UTF-8)');

my $json = do { local $/; <STDIN> };
my ($path) = $json =~ /"file_path"\s*:\s*"((?:[^"\\]|\\.)*)"/;
exit 0 unless defined $path;
$path =~ s/\\\\/\\/g;
exit 0 unless $path =~ /\.(kt|kts|sql)$/;
exit 0 unless -f $path;

my $is_sql = $path =~ /\.sql$/;
open(my $fh, '<:encoding(UTF-8)', $path) or exit 0;

my @problems;
my $line_number = 0;
while (my $line = <$fh>) {
    $line_number++;
    chomp $line;

    my $comment;
    if ($is_sql && $line =~ m{^\s*--\s?(.*)$}) {
        $comment = $1;
    } elsif ($line =~ m{^\s*//\s?(.*)$}) {
        $comment = $1;
    } elsif ($line =~ m{^\s*\*(?!/)\s?(.*)$}) {
        $comment = $1;
    } elsif ($line =~ m{^([^"]*)//\s?(.*)$} && ($1 =~ tr/"//) % 2 == 0) {
        $comment = $2;
    }
    next unless defined $comment;
    next unless $comment =~ /\p{Hangul}/;

    push @problems, "$path:$line_number  주석 끝에 마침표 - 떼어낼 것\n    $comment"
        if $comment =~ /\.\s*$/;
    push @problems, "$path:$line_number  주석 안 마침표 - 줄바꿈으로 나눌 것\n    $comment"
        if $comment =~ /\p{Hangul}\.\s+\S/;
    push @problems, "$path:$line_number  가운뎃점 - 쉼표나 '와'로 바꿀 것\n    $comment"
        if $comment =~ /\x{00B7}/;
    push @problems, "$path:$line_number  서술형 종결어미 - 명사형(-함/-됨/-임)으로 쓸 것\n    $comment"
        if $comment =~ /(한다|된다|이다|없다|있다|간다|온다|준다|짓는다)\s*\.?\s*$/;
    push @problems, "$path:$line_number  영문 뒤 조사는 붙여쓸 것\n    $comment"
        if $comment =~ /[A-Za-z0-9\)] (?:은|는|이|가|을|를|과|와|의|도|만|에|에서|으로|로|부터|까지|보다|처럼)(?=[\s,\)]|$)/;
}
close($fh);

if (@problems) {
    print STDERR "CLAUDE.md 6절 주석 컨벤션 위반:\n\n" . join("\n", @problems) . "\n";
    exit 2;
}
exit 0;
