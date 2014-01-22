function [n,l,f,r]=plot_xopt(folder,scriptname)

here = fileparts(mfilename('fullpath'));

cd(folder)
eval(['[n,l,f,r]=' scriptname '();'])
cd(here)

t = 1:size(n,2);

figure('Position',[403    33   560   633])
subplot(411)
plot(t,n)
ylabel('n')
subplot(412)
plot(t,f)
ylabel('f')
subplot(413)
plot(t,l)
ylabel('l')
subplot(414)
plot(t,r)
ylabel('r')