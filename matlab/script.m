function [n,l,f,r]=script()

close all

here = fileparts(mfilename('fullpath'));

folder = fullfile(fileparts(here),'out');
filename = 'sol_us';
[n,l,f,r]=plot_xopt(folder,filename);